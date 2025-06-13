import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';
import { devtools, persist } from 'zustand/middleware';
import { 
  GameQuestion, 
  GameResult, 
  Difficulty, 
  AnswerResponse, 
  RelationshipStep,
  PersonInfo,
  HighScores
} from '../types/game';

interface GameState {
  // Core game data
  currentQuestion: GameQuestion | null;
  score: number;
  streak: number;
  maxStreak: number;
  questionsAnswered: number;
  correctAnswers: number;
  difficulty: Difficulty;
  playerName: string;
  
  // Game flow
  isGameActive: boolean;
  isLoading: boolean;
  error: string | null;
  gameStartTime: number | null;
  
  // High scores
  highScores: HighScores;
  
  // Performance tracking
  performanceMetrics: {
    lastQuestionTime: number;
    averageResponseTime: number;
    fastestResponse: number;
    slowestResponse: number;
    totalResponseTime: number;
  };
  
  // UI state
  showHints: boolean;
  selectedLanguage: string;
}

interface GameActions {
  // Core actions
  setCurrentQuestion: (question: GameQuestion | null) => void;
  setScore: (score: number) => void;
  setStreak: (streak: number) => void;
  setMaxStreak: (maxStreak: number) => void;
  setQuestionsAnswered: (count: number) => void;
  setCorrectAnswers: (count: number) => void;
  setDifficulty: (difficulty: Difficulty) => void;
  setPlayerName: (name: string) => void;
  
  // Game flow actions
  setIsGameActive: (active: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  startGame: (playerName: string, difficulty: Difficulty) => void;
  endGame: () => void;
  
  // High scores
  setHighScores: (scores: HighScores) => void;
  
  // Performance tracking
  recordResponseTime: (responseTime: number) => void;
  resetPerformanceMetrics: () => void;
  
  // UI actions
  toggleHints: () => void;
  setLanguage: (lang: string) => void;
  
  // Utility actions
  resetGame: () => void;
  getGameStats: () => GameStats;
}

interface GameStats {
  accuracy: number;
  averageResponseTime: number;
  gameProgress: number;
  performanceRating: 'Excellent' | 'Good' | 'Average' | 'Needs Improvement';
}

type GameStore = GameState & GameActions;

const initialState: GameState = {
  currentQuestion: null,
  score: 0,
  streak: 0,
  maxStreak: 0,
  questionsAnswered: 0,
  correctAnswers: 0,
  difficulty: Difficulty.MEDIUM,
  playerName: '',
  
  isGameActive: false,
  isLoading: false,
  error: null,
  gameStartTime: null,
  
  highScores: {
    [Difficulty.EASY]: [],
    [Difficulty.MEDIUM]: [],
    [Difficulty.HARD]: [],
  },
  
  performanceMetrics: {
    lastQuestionTime: 0,
    averageResponseTime: 0,
    fastestResponse: Number.MAX_VALUE,
    slowestResponse: 0,
    totalResponseTime: 0,
  },
  
  showHints: false,
  selectedLanguage: 'tr',
};

export const useGameStore = create<GameStore>()(
  devtools(
    persist(
      subscribeWithSelector((set, get) => ({
        ...initialState,
        
        // Core actions
        setCurrentQuestion: (question) => set({ currentQuestion: question }),
        setScore: (score) => set({ score }),
        setStreak: (streak) => {
          const currentMaxStreak = get().maxStreak;
          set({ 
            streak,
            maxStreak: Math.max(streak, currentMaxStreak)
          });
        },
        setMaxStreak: (maxStreak) => set({ maxStreak }),
        setQuestionsAnswered: (count) => set({ questionsAnswered: count }),
        setCorrectAnswers: (count) => set({ correctAnswers: count }),
        setDifficulty: (difficulty) => set({ difficulty }),
        setPlayerName: (name) => set({ playerName: name.trim() }),
        
        // Game flow actions
        setIsGameActive: (active) => set({ isGameActive: active }),
        setLoading: (loading) => set({ isLoading: loading }),
        setError: (error) => set({ error }),
        
        startGame: (playerName, difficulty) => {
          set({ 
            playerName: playerName.trim(),
            difficulty,
            isGameActive: true,
            gameStartTime: Date.now(),
            score: 0,
            streak: 0,
            maxStreak: 0,
            questionsAnswered: 0,
            correctAnswers: 0,
            error: null,
            performanceMetrics: {
              lastQuestionTime: Date.now(),
              averageResponseTime: 0,
              fastestResponse: Number.MAX_VALUE,
              slowestResponse: 0,
              totalResponseTime: 0,
            },
          });
        },
        
        endGame: () => {
          set({
            isGameActive: false,
            currentQuestion: null,
            gameStartTime: null,
          });
        },
        
        // High scores
        setHighScores: (scores) => set({ highScores: scores }),
        
        // Performance tracking
        recordResponseTime: (responseTime) => {
          const metrics = get().performanceMetrics;
          const questionsAnswered = get().questionsAnswered;
          
          const newTotalTime = metrics.totalResponseTime + responseTime;
          const newAverageTime = questionsAnswered > 0 ? newTotalTime / questionsAnswered : 0;
          
          set({ 
            performanceMetrics: {
              lastQuestionTime: Date.now(),
              averageResponseTime: newAverageTime,
              fastestResponse: Math.min(metrics.fastestResponse, responseTime),
              slowestResponse: Math.max(metrics.slowestResponse, responseTime),
              totalResponseTime: newTotalTime,
            },
          });
        },
        
        resetPerformanceMetrics: () => {
          set({
            performanceMetrics: {
              lastQuestionTime: 0,
              averageResponseTime: 0,
              fastestResponse: Number.MAX_VALUE,
              slowestResponse: 0,
              totalResponseTime: 0,
            },
          });
        },
        
        // UI actions
        toggleHints: () => set((state) => ({ showHints: !state.showHints })),
        setLanguage: (lang) => set({ selectedLanguage: lang }),
        
        // Utility actions
        resetGame: () => {
          set({
            ...initialState,
            selectedLanguage: get().selectedLanguage, // Preserve language preference
            highScores: get().highScores, // Preserve high scores
          });
        },
        
        getGameStats: () => {
          const state = get();
          const accuracy = state.questionsAnswered > 0 
            ? Math.round((state.correctAnswers / state.questionsAnswered) * 100) 
            : 0;
          
          const gameProgress = state.questionsAnswered / 10; // Assuming 10 questions per game
          
          let performanceRating: GameStats['performanceRating'] = 'Needs Improvement';
          if (accuracy >= 90 && state.performanceMetrics.averageResponseTime < 10) {
            performanceRating = 'Excellent';
          } else if (accuracy >= 75 && state.performanceMetrics.averageResponseTime < 15) {
            performanceRating = 'Good';
          } else if (accuracy >= 60) {
            performanceRating = 'Average';
          }
          
          return {
            accuracy,
            averageResponseTime: state.performanceMetrics.averageResponseTime,
            gameProgress,
            performanceRating,
          };
        },
      })),
      {
        name: 'family-game-store',
        partialize: (state) => ({
          highScores: state.highScores,
          selectedLanguage: state.selectedLanguage,
          showHints: state.showHints,
        }),
      }
    ),
    {
      name: 'family-game-store',
    }
  )
);

// Selectors for optimized component updates
export const selectGameState = (state: GameStore) => ({
  currentQuestion: state.currentQuestion,
  score: state.score,
  streak: state.streak,
  isGameActive: state.isGameActive,
  isLoading: state.isLoading,
  error: state.error,
});

export const selectGameStats = (state: GameStore) => state.getGameStats();

export const selectPerformanceMetrics = (state: GameStore) => state.performanceMetrics; 