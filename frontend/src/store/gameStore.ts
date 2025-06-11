import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { 
  GameQuestion, 
  GameResult, 
  Difficulty, 
  AnswerResponse, 
  RelationshipStep,
  PersonInfo 
} from '../types/game';

export interface GameSession {
  sessionId: string;
  playerName: string;
  difficulty: Difficulty;
  startTime: number;
  currentScore: number;
  currentStreak: number;
  maxStreak: number;
  questionsAnswered: number;
  correctAnswers: number;
  totalQuestions: number;
  timeRemaining: number;
  isActive: boolean;
  isPaused: boolean;
  askedQuestions: string[];
}

export interface GameState {
  // Session data
  currentSession: GameSession | null;
  
  // Current game state
  currentQuestion: GameQuestion | null;
  selectedAnswer: string;
  showResult: boolean;
  isCorrect: boolean;
  isLoading: boolean;
  error: string | null;
  
  // UI state
  showHint: boolean;
  showScores: boolean;
  isPathVisible: boolean;
  
  // Game flow
  gameStarted: boolean;
  gameOver: boolean;
  finalResult: GameResult | null;
  lastAnswerResponse: AnswerResponse | null;
  currentRelationshipPath: RelationshipStep[] | undefined;
  
  // Player preferences
  playerName: string;
  selectedDifficulty: Difficulty;
  language: string;
}

export interface GameActions {
  // Session actions
  createSession: (playerName: string, difficulty: Difficulty) => void;
  updateSession: (updates: Partial<GameSession>) => void;
  endSession: () => void;
  pauseGame: () => void;
  resumeGame: () => void;
  
  // Game flow actions
  startGame: () => void;
  setCurrentQuestion: (question: GameQuestion | null) => void;
  selectAnswer: (answer: string) => void;
  submitAnswer: (answer: string) => Promise<void>;
  nextQuestion: () => void;
  restartGame: () => void;
  
  // Timer actions
  updateTimeRemaining: (time: number) => void;
  resetTimer: () => void;
  
  // Score actions
  updateScore: (score: number) => void;
  updateStreak: (streak: number) => void;
  incrementCorrectAnswers: () => void;
  
  // UI actions
  setShowResult: (show: boolean) => void;
  setShowHint: (show: boolean) => void;
  setShowScores: (show: boolean) => void;
  setPathVisible: (visible: boolean) => void;
  setError: (error: string | null) => void;
  setLoading: (loading: boolean) => void;
  
  // Player preferences
  setPlayerName: (name: string) => void;
  setDifficulty: (difficulty: Difficulty) => void;
  setLanguage: (language: string) => void;
  
  // Advanced actions
  addAskedQuestion: (questionId: string) => void;
  setLastAnswerResponse: (response: AnswerResponse | null) => void;
  setCurrentRelationshipPath: (path: RelationshipStep[] | undefined) => void;
  setFinalResult: (result: GameResult | null) => void;
}

type GameStore = GameState & GameActions;

const initialState: GameState = {
  currentSession: null,
  currentQuestion: null,
  selectedAnswer: '',
  showResult: false,
  isCorrect: false,
  isLoading: false,
  error: null,
  showHint: false,
  showScores: false,
  isPathVisible: false,
  gameStarted: false,
  gameOver: false,
  finalResult: null,
  lastAnswerResponse: null,
  currentRelationshipPath: undefined,
  playerName: localStorage.getItem('playerName') || '',
  selectedDifficulty: Difficulty.MEDIUM,
  language: localStorage.getItem('i18nextLng') || 'tr',
};

export const useGameStore = create<GameStore>()(
  devtools(
    persist(
      (set, get) => ({
        ...initialState,
        
        // Session actions
        createSession: (playerName: string, difficulty: Difficulty) => {
          const sessionId = `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
          const session: GameSession = {
            sessionId,
            playerName,
            difficulty,
            startTime: Date.now(),
            currentScore: 0,
            currentStreak: 0,
            maxStreak: 0,
            questionsAnswered: 0,
            correctAnswers: 0,
            totalQuestions: 0,
            timeRemaining: 30,
            isActive: true,
            isPaused: false,
            askedQuestions: [],
          };
          
          localStorage.setItem('gameSessionId', sessionId);
          set({ currentSession: session, playerName, selectedDifficulty: difficulty });
        },
        
        updateSession: (updates: Partial<GameSession>) => {
          const currentSession = get().currentSession;
          if (currentSession) {
            const updatedSession = { ...currentSession, ...updates };
            // SessionId güncellenirse localStorage'a da kaydet
            if (updates.sessionId) {
              localStorage.setItem('gameSessionId', updates.sessionId);
            }
            set({ currentSession: updatedSession });
          }
        },
        
        endSession: () => {
          localStorage.removeItem('gameSessionId');
          set({ 
            currentSession: null, 
            gameStarted: false, 
            gameOver: true,
            currentQuestion: null
          });
        },
        
        pauseGame: () => {
          const session = get().currentSession;
          if (session) {
            set({ currentSession: { ...session, isPaused: true } });
          }
        },
        
        resumeGame: () => {
          const session = get().currentSession;
          if (session) {
            set({ currentSession: { ...session, isPaused: false } });
          }
        },
        
        // Game flow actions
        startGame: () => {
          set({ 
            gameStarted: true, 
            gameOver: false, 
            error: null,
            showResult: false,
            selectedAnswer: '',
            currentQuestion: null
          });
        },
        
        setCurrentQuestion: (question: GameQuestion | null) => {
          set({ currentQuestion: question });
          if (question) {
            get().addAskedQuestion(question.id.toString());
            const session = get().currentSession;
            if (session) {
              set({ 
                currentSession: { 
                  ...session, 
                  totalQuestions: session.totalQuestions + 1 
                }
              });
            }
          }
        },
        
        selectAnswer: (answer: string) => {
          set({ selectedAnswer: answer });
        },
        
        submitAnswer: async (answer: string) => {
          set({ isLoading: true, error: null });
          // Bu fonksiyon component'te implement edilecek
          // Store sadece state'i yönetir, API çağrıları component'te yapılır
        },
        
        nextQuestion: () => {
          set({ 
            showResult: false, 
            selectedAnswer: '', 
            lastAnswerResponse: null,
            currentRelationshipPath: undefined,
            showHint: false 
          });
        },
        
        restartGame: () => {
          set({
            ...initialState,
            playerName: get().playerName,
            selectedDifficulty: get().selectedDifficulty,
            language: get().language,
          });
          localStorage.removeItem('gameSessionId');
        },
        
        // Timer actions
        updateTimeRemaining: (time: number) => {
          const session = get().currentSession;
          if (session) {
            set({ currentSession: { ...session, timeRemaining: time } });
          }
        },
        
        resetTimer: () => {
          const question = get().currentQuestion;
          const timeLimit = question?.timeLimit || 30;
          get().updateTimeRemaining(timeLimit);
        },
        
        // Score actions
        updateScore: (score: number) => {
          const session = get().currentSession;
          if (session) {
            set({ currentSession: { ...session, currentScore: score } });
          }
        },
        
        updateStreak: (streak: number) => {
          const session = get().currentSession;
          if (session) {
            const newMaxStreak = Math.max(session.maxStreak, streak);
            set({ 
              currentSession: { 
                ...session, 
                currentStreak: streak,
                maxStreak: newMaxStreak
              } 
            });
          }
        },
        
        incrementCorrectAnswers: () => {
          const session = get().currentSession;
          if (session) {
            set({ 
              currentSession: { 
                ...session, 
                correctAnswers: session.correctAnswers + 1,
                questionsAnswered: session.questionsAnswered + 1
              } 
            });
          }
        },
        
        // UI actions
        setShowResult: (show: boolean) => set({ showResult: show }),
        setShowHint: (show: boolean) => set({ showHint: show }),
        setShowScores: (show: boolean) => set({ showScores: show }),
        setPathVisible: (visible: boolean) => set({ isPathVisible: visible }),
        setError: (error: string | null) => set({ error }),
        setLoading: (loading: boolean) => set({ isLoading: loading }),
        
        // Player preferences
        setPlayerName: (name: string) => {
          localStorage.setItem('playerName', name);
          set({ playerName: name });
        },
        
        setDifficulty: (difficulty: Difficulty) => {
          set({ selectedDifficulty: difficulty });
        },
        
        setLanguage: (language: string) => {
          localStorage.setItem('i18nextLng', language);
          set({ language });
        },
        
        // Advanced actions
        addAskedQuestion: (questionId: string) => {
          const session = get().currentSession;
          if (session && !session.askedQuestions.includes(questionId)) {
            set({ 
              currentSession: { 
                ...session, 
                askedQuestions: [...session.askedQuestions, questionId] 
              } 
            });
          }
        },
        
        setLastAnswerResponse: (response: AnswerResponse | null) => {
          set({ lastAnswerResponse: response });
        },
        
        setCurrentRelationshipPath: (path: RelationshipStep[] | undefined) => {
          set({ currentRelationshipPath: path });
        },
        
        setFinalResult: (result: GameResult | null) => {
          set({ finalResult: result });
        },
      }),
      {
        name: 'family-tree-game-store',
        partialize: (state) => ({
          playerName: state.playerName,
          selectedDifficulty: state.selectedDifficulty,
          language: state.language,
          currentSession: state.currentSession,
        }),
      }
    ),
    { name: 'GameStore' }
  )
); 