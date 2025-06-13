import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { Difficulty, GameQuestion, AnswerResponse } from '../types/game';
import { toast } from 'react-hot-toast';

interface GameContextType {
  // Game State
  playerName: string;
  setPlayerName: (name: string) => void;
  currentDifficulty: Difficulty;
  setCurrentDifficulty: (difficulty: Difficulty) => void;
  gameStarted: boolean;
  setGameStarted: (started: boolean) => void;
  gameOver: boolean;
  setGameOver: (over: boolean) => void;
  
  // Question State
  currentQuestion: GameQuestion | null;
  setCurrentQuestion: (question: GameQuestion | null) => void;
  
  // Game Progress
  currentScore: number;
  setCurrentScore: (score: number) => void;
  correctAnswers: number;
  setCorrectAnswers: (count: number) => void;
  questionCount: number;
  setQuestionCount: (count: number) => void;
  totalQuestions: number;
  setTotalQuestions: (total: number) => void;
  currentStreak: number;
  setCurrentStreak: (streak: number) => void;
  maxStreak: number;
  setMaxStreak: (streak: number) => void;
  
  // Answer Response
  lastAnswerResponse: AnswerResponse | null;
  setLastAnswerResponse: (response: AnswerResponse | null) => void;
  
  // Timer
  timeLeft: number;
  setTimeLeft: (time: number) => void;
  questionTimeLeft: number;
  setQuestionTimeLeft: (time: number) => void;
  
  // Game Control
  isGameActive: boolean;
  setIsGameActive: (active: boolean) => void;
  canAnswer: boolean;
  setCanAnswer: (can: boolean) => void;
  
  // Utility Functions
  resetGame: () => void;
  addScore: (points: number) => void;
  incrementCorrectAnswers: () => void;
  updateStreak: (isCorrect: boolean) => void;
}

const GameContext = createContext<GameContextType | undefined>(undefined);

export const GameProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Basic game state
  const [playerName, setPlayerName] = useState<string>('');
  const [currentDifficulty, setCurrentDifficulty] = useState<Difficulty>(Difficulty.MEDIUM);
  const [gameStarted, setGameStarted] = useState<boolean>(false);
  const [gameOver, setGameOver] = useState<boolean>(false);
  
  // Question state
  const [currentQuestion, setCurrentQuestion] = useState<GameQuestion | null>(null);
  
  // Game progress
  const [currentScore, setCurrentScore] = useState<number>(0);
  const [correctAnswers, setCorrectAnswers] = useState<number>(0);
  const [questionCount, setQuestionCount] = useState<number>(0);
  const [totalQuestions, setTotalQuestions] = useState<number>(10);
  const [currentStreak, setCurrentStreak] = useState<number>(0);
  const [maxStreak, setMaxStreak] = useState<number>(0);
  
  // Answer response
  const [lastAnswerResponse, setLastAnswerResponse] = useState<AnswerResponse | null>(null);
  
  // Timer
  const [timeLeft, setTimeLeft] = useState<number>(180);
  const [questionTimeLeft, setQuestionTimeLeft] = useState<number>(20);
  
  // Game control
  const [isGameActive, setIsGameActive] = useState<boolean>(false);
  const [canAnswer, setCanAnswer] = useState<boolean>(true);
  
  // Refs for cleanup
  const gameTimerRef = useRef<NodeJS.Timeout | null>(null);
  const questionTimerRef = useRef<NodeJS.Timeout | null>(null);
  
  // Utility functions
  const addScore = useCallback((points: number) => {
    setCurrentScore(prev => prev + points);
  }, []);
  
  const incrementCorrectAnswers = useCallback(() => {
    setCorrectAnswers(prev => prev + 1);
  }, []);
  
  const updateStreak = useCallback((isCorrect: boolean) => {
    if (isCorrect) {
      setCurrentStreak(prev => {
        const newStreak = prev + 1;
        setMaxStreak(current => Math.max(current, newStreak));
        return newStreak;
      });
    } else {
      setCurrentStreak(0);
    }
  }, []);
  
  const resetGame = useCallback(() => {
    // Clear timers
    if (gameTimerRef.current) {
      clearInterval(gameTimerRef.current);
      gameTimerRef.current = null;
    }
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
      questionTimerRef.current = null;
    }
    
    // Reset all state
    setGameStarted(false);
    setGameOver(false);
    setCurrentQuestion(null);
    setCurrentScore(0);
    setCorrectAnswers(0);
    setQuestionCount(0);
    setCurrentStreak(0);
    setMaxStreak(0);
    setLastAnswerResponse(null);
    setTimeLeft(180);
    setQuestionTimeLeft(20);
    setIsGameActive(false);
    setCanAnswer(true);
  }, []);
  
  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (gameTimerRef.current) {
        clearInterval(gameTimerRef.current);
      }
      if (questionTimerRef.current) {
        clearInterval(questionTimerRef.current);
      }
    };
  }, []);
  
  const contextValue: GameContextType = {
    // Game State
    playerName,
    setPlayerName,
    currentDifficulty,
    setCurrentDifficulty,
    gameStarted,
    setGameStarted,
    gameOver,
    setGameOver,
    
    // Question State
    currentQuestion,
    setCurrentQuestion,
    
    // Game Progress
    currentScore,
    setCurrentScore,
    correctAnswers,
    setCorrectAnswers,
    questionCount,
    setQuestionCount,
    totalQuestions,
    setTotalQuestions,
    currentStreak,
    setCurrentStreak,
    maxStreak,
    setMaxStreak,
    
    // Answer Response
    lastAnswerResponse,
    setLastAnswerResponse,
    
    // Timer
    timeLeft,
    setTimeLeft,
    questionTimeLeft,
    setQuestionTimeLeft,
    
    // Game Control
    isGameActive,
    setIsGameActive,
    canAnswer,
    setCanAnswer,
    
    // Utility Functions
    resetGame,
    addScore,
    incrementCorrectAnswers,
    updateStreak,
  };
  
  return (
    <GameContext.Provider value={contextValue}>
      {children}
    </GameContext.Provider>
  );
};

export const useGameContext = () => {
  const context = useContext(GameContext);
  if (context === undefined) {
    throw new Error('useGameContext must be used within a GameProvider');
  }
  return context;
}; 