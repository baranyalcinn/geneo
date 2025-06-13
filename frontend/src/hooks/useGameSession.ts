import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useGameStore } from '../store/gameStore';
import * as gameService from '../services/gameService';
import { Difficulty } from '../types/game';
import toast from 'react-hot-toast';

export const useGameSession = () => {
  const [timeLeft, setTimeLeft] = useState(180);
  const [questionTimeLeft, setQuestionTimeLeft] = useState(20);
  const [isGameActive, setIsGameActive] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [canAnswer, setCanAnswer] = useState(true);
  const [questionCount, setQuestionCount] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(10);
  const [currentQuestion, setCurrentQuestion] = useState<any>(null);
  const [playerName, setPlayerName] = useState('');
  const [currentDifficulty, setCurrentDifficulty] = useState<Difficulty>(Difficulty.MEDIUM);
  const [gameStarted, setGameStarted] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const [currentScore, setCurrentScore] = useState(0);
  const [currentStreak, setCurrentStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [correctAnswers, setCorrectAnswers] = useState(0);

  // Performance için memoized refs
  const gameTimerRef = useRef<NodeJS.Timeout | null>(null);
  const questionTimerRef = useRef<NodeJS.Timeout | null>(null);
  const questionStartTimeRef = useRef<number>(0);
  const mountedRef = useRef<boolean>(true);
  
  // Cleanup için refs
  const cleanupRef = useRef<(() => void) | null>(null);
  
  // Refs for storing callback functions to avoid hoisting issues
  const nextQuestionRef = useRef<(() => void) | null>(null);
  const handleQuestionTimeoutRef = useRef<(() => void) | null>(null);
  const startQuestionTimerRef = useRef<((timeLimit?: number) => void) | null>(null);
  const endGameSessionRef = useRef<((reason: 'completed' | 'time' | 'manual') => Promise<void>) | null>(null);
  
  const {
    setLoading,
    error,
    setError,
  } = useGameStore();

  // Memoized values for performance
  const gameStats = useMemo(() => ({
    currentScore,
    currentStreak,
    maxStreak,
    correctAnswers,
    questionsAnswered: questionCount,
    accuracy: questionCount > 0 ? Math.round((correctAnswers / questionCount) * 100) : 0
  }), [currentScore, currentStreak, maxStreak, correctAnswers, questionCount]);

  const timeDisplays = useMemo(() => ({
    gameTimeFormatted: formatTime(timeLeft),
    questionTimeFormatted: formatTime(questionTimeLeft),
    isGameTimeCritical: timeLeft <= 30,
    isQuestionTimeCritical: questionTimeLeft <= 5
  }), [timeLeft, questionTimeLeft]);

  // Helper function for time formatting
  const formatTime = useCallback((seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }, []);

  // Enhanced cleanup function
  const cleanup = useCallback(() => {
    if (gameTimerRef.current) {
      clearInterval(gameTimerRef.current);
      gameTimerRef.current = null;
    }
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
      questionTimerRef.current = null;
    }
    
    // Reset states
    setIsGameActive(false);
    setCanAnswer(false);
    setIsPaused(false);
  }, []);

  // Component unmount cleanup
  useEffect(() => {
    mountedRef.current = true;
    cleanupRef.current = cleanup;
    
    return () => {
      mountedRef.current = false;
      cleanup();
    };
  }, [cleanup]);

  const endGameSession = useCallback(async (reason: 'completed' | 'time' | 'manual') => {
    if (!mountedRef.current) return;
    
    try {
      console.log(`Oyun bitiriliyor: ${reason}`);
      
      cleanup();
      setGameOver(true);
      
      if (reason === 'time') {
        toast.error('⏰ Süre doldu! Oyun bitti.', { duration: 3000 });
      } else if (reason === 'completed') {
        toast.success('🎉 Tebrikler! Oyunu tamamladınız!', { duration: 3000 });
      }
      
      // Oyun sonunda score kaydet
      if (currentScore > 0 && playerName.trim()) {
        try {
          const scoreData = {
            playerName: playerName.trim(),
            score: currentScore,
            difficulty: currentDifficulty,
            correctAnswers: correctAnswers,
            totalQuestions: totalQuestions,
            maxStreak: maxStreak
          };
          
          await gameService.recordGameResult(scoreData, 'tr');
          if (mountedRef.current) {
            toast.success('🏆 Skorunuz kaydedildi!', { duration: 2000 });
          }
        } catch (error) {
          console.error('Skor kaydetme hatası:', error);
          if (mountedRef.current) {
            toast.error('Skor kaydedilemedi');
          }
        }
      }
      
    } catch (error: any) {
      console.error('Oyun bitirme hatası:', error);
      if (mountedRef.current) {
        setError(error.message || 'Oyun bitirilemedi');
      }
    }
  }, [currentScore, playerName, currentDifficulty, correctAnswers, totalQuestions, maxStreak, setError, cleanup]);

  const startQuestionTimer = useCallback((timeLimit: number = 20) => {
    if (!mountedRef.current) return;
    
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    setQuestionTimeLeft(timeLimit);
    setCanAnswer(true);
    questionStartTimeRef.current = Date.now();
    
    questionTimerRef.current = setInterval(() => {
      if (!mountedRef.current) {
        if (questionTimerRef.current) {
          clearInterval(questionTimerRef.current);
        }
        return;
      }
      
      setQuestionTimeLeft((prev) => {
        if (prev <= 1) {
          if (handleQuestionTimeoutRef.current) {
            handleQuestionTimeoutRef.current();
          }
          return 0;
        }
        
        // Performance improvement: Less frequent toast notifications
        if (prev === 3) {
          toast('⏰ 3 saniye kaldı!', { duration: 1500, icon: '⚠️' });
        }
        
        return prev - 1;
      });
    }, 1000);
  }, []);

  const startGameTimer = useCallback(() => {
    if (gameTimerRef.current) {
      clearInterval(gameTimerRef.current);
    }
    
    gameTimerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          if (endGameSessionRef.current) {
            endGameSessionRef.current('time');
          }
          return 0;
        }
        
        // Sadece gerçekten kritik anlarda uyarı ver
        if (prev === 15) {
          toast.error('⏰ Son 15 saniye!', { 
            duration: 2000,
            icon: '🚨' 
          });
        }
        
        return prev - 1;
      });
    }, 1000);
  }, []);

  const nextQuestion = useCallback(() => {
    // Eğer toplam soru sayısına ulaştıysak oyunu bitir
    if (questionCount >= totalQuestions) {
      if (endGameSessionRef.current) {
        endGameSessionRef.current('completed');
      }
      return;
    }
    
    // Timer'ları temizle
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    // Yeni soru için timer başlat
    let timeLimit = 15; // HARD difficulty default
    if (currentDifficulty === Difficulty.EASY) {
      timeLimit = 20;
    } else if (currentDifficulty === Difficulty.MEDIUM) {
      timeLimit = 18;
    }
    
    if (startQuestionTimerRef.current) {
      startQuestionTimerRef.current(timeLimit);
    }
  }, [questionCount, totalQuestions, currentDifficulty]);

  const handleQuestionTimeout = useCallback(() => {
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    setCanAnswer(false);
    toast.error('⏰ Süre doldu! Sonraki soruya geçiliyor...', { 
      duration: 2000,
      icon: '⏳' 
    });
    
    // Soru sayısını artır
    setQuestionCount(prev => prev + 1);
    
    setTimeout(() => {
      if (nextQuestionRef.current) {
        nextQuestionRef.current();
      }
    }, 2000);
  }, []);

  // Ref'leri useEffect içinde güncelle
  useEffect(() => {
    endGameSessionRef.current = endGameSession;
    startQuestionTimerRef.current = startQuestionTimer;
    nextQuestionRef.current = nextQuestion;
    handleQuestionTimeoutRef.current = handleQuestionTimeout;
  }, [endGameSession, startQuestionTimer, nextQuestion, handleQuestionTimeout]);

  const startGame = useCallback(async (name: string, difficulty: Difficulty) => {
    try {
      setLoading(true);
      setError(null);
      
      const initialData = await gameService.startGameSimple(name, difficulty);
      
      // Game state'i başlat
      setPlayerName(name);
      setCurrentDifficulty(difficulty);
      setTotalQuestions(initialData.totalQuestions);
      setCurrentQuestion(initialData.firstQuestion);
      setQuestionCount(1);
      setTimeLeft(initialData.gameDurationInSeconds);
      setIsGameActive(true);
      setCanAnswer(true);
      setGameStarted(true);
      setGameOver(false);
      setCurrentScore(0);
      setCurrentStreak(0);
      setMaxStreak(0);
      setCorrectAnswers(0);

      
      startGameTimer();
      
      let timeLimit = 15; // HARD difficulty default
      if (initialData.difficulty === Difficulty.EASY) {
        timeLimit = 20;
      } else if (initialData.difficulty === Difficulty.MEDIUM) {
        timeLimit = 18;
      }
      
      startQuestionTimer(timeLimit);
      
    } catch (error: any) {
      console.error('Oyun başlatma hatası:', error);
      setError(error.message || 'Oyun başlatılamadı');
      toast.error(`Hata: ${error.message || 'Oyun başlatılamadı'}`);
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError, startGameTimer, startQuestionTimer]);

  const answerQuestion = useCallback((answer: string, isCorrect: boolean, pointsEarned: number = 0) => {
    if (!canAnswer) {
      console.warn("Cevap verilemiyor: canAnswer =", canAnswer);
      return;
    }
    
    setCanAnswer(false);
    
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    // Score güncelle
    setCurrentScore(prev => prev + pointsEarned);
    
    // Streak güncelle
    if (isCorrect) {
      setCurrentStreak(prev => prev + 1);
      setMaxStreak(prev => Math.max(prev, currentStreak + 1));
      setCorrectAnswers(prev => prev + 1);
    } else {
      setCurrentStreak(0);
    }
    
    // Soru sayısını artır
    setQuestionCount(prev => {
      const newCount = prev + 1;
      console.log(`Soru sayısı güncellendi: ${prev} -> ${newCount} / ${totalQuestions}`);
      return newCount;
    });
    
  }, [canAnswer, currentStreak, totalQuestions]);

  const pauseGame = useCallback(() => {
    setIsPaused(true);
    
    if (gameTimerRef.current) {
      clearInterval(gameTimerRef.current);
    }
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    toast('⏸️ Oyun duraklatıldı', { duration: 2000 });
  }, []);

  const resumeGame = useCallback(() => {
    setIsPaused(false);
    startGameTimer();
    
    if (questionTimeLeft > 0) {
      startQuestionTimer(questionTimeLeft);
    }
    
    toast('▶️ Oyun devam ediyor', { duration: 2000 });
  }, [questionTimeLeft, startGameTimer, startQuestionTimer]);

  const restartGame = useCallback(() => {
    // Tüm state'i sıfırla
    setIsGameActive(false);
    setGameStarted(false);
    setGameOver(false);
    setCurrentQuestion(null);
    setQuestionCount(0);
    setTimeLeft(180);
    setQuestionTimeLeft(20);
    setCanAnswer(true);
    setCurrentScore(0);
    setCurrentStreak(0);
    setMaxStreak(0);
    setCorrectAnswers(0);

    setError(null);
    
    // Timer'ları temizle
    if (gameTimerRef.current) {
      clearInterval(gameTimerRef.current);
    }
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
  }, [setError]);

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

  return {
    // Game state
    isGameActive,
    isPaused,
    gameStarted,
    gameOver,
    
    // Question state
    currentQuestion,
    setCurrentQuestion,
    questionCount,
    totalQuestions,
    canAnswer,
    
    // Timer state
    timeLeft,
    questionTimeLeft,
    
    // Score state
    currentScore,
    currentStreak,
    maxStreak,
    correctAnswers,
    
    // Player state
    playerName,
    currentDifficulty,
    
    // Actions
    startGame,
    answerQuestion,
    nextQuestion,
    endGameSession,
    pauseGame,
    resumeGame,
    restartGame,
    startQuestionTimer,
    
    // Error state
    error,
    
    // Progress helpers
    getProgress: () => (questionCount / totalQuestions) * 100,
    getTimeProgress: () => (timeLeft / 180) * 100,
    getQuestionTimeProgress: () => {
      let timeLimit = 15;
      if (currentDifficulty === Difficulty.EASY) {
        timeLimit = 20;
      } else if (currentDifficulty === Difficulty.MEDIUM) {
        timeLimit = 18;
      }
      return (questionTimeLeft / timeLimit) * 100;
    },
    getAccuracy: () => (correctAnswers / Math.max(questionCount - 1, 1)) * 100,
  };
};
