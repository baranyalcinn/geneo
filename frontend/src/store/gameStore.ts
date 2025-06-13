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

export interface GameState {
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
  // Game flow actions
  startGame: () => void;
  setCurrentQuestion: (question: GameQuestion | null) => void;
  selectAnswer: (answer: string) => void;
  nextQuestion: () => void;
  restartGame: () => void;
  
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
  setLastAnswerResponse: (response: AnswerResponse | null) => void;
  setCurrentRelationshipPath: (path: RelationshipStep[] | undefined) => void;
  setFinalResult: (result: GameResult | null) => void;
}

type GameStore = GameState & GameActions;

const initialState: GameState = {
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
        },
        
        selectAnswer: (answer: string) => {
          set({ selectedAnswer: answer });
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
        }),
      }
    ),
    { name: 'GameStore' }
  )
); 