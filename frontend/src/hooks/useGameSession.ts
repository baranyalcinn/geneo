import { useState, useEffect, useCallback, useRef } from 'react';
import { useGameStore } from '../store/gameStore';
import * as gameService from '../services/gameService';
import { Difficulty, GameAnalysis } from '../types/game';
import toast from 'react-hot-toast';

export const useGameSession = () => {
  const [timeLeft, setTimeLeft] = useState(180);
  const [questionTimeLeft, setQuestionTimeLeft] = useState(20);
  const [isGameActive, setIsGameActive] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [showAnalysis, setShowAnalysis] = useState(false);
  const [gameAnalysis, setGameAnalysis] = useState<GameAnalysis | null>(null);
  const [canAnswer, setCanAnswer] = useState(true);
  const [questionCount, setQuestionCount] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(10);
  const [currentQuestion, setCurrentQuestion] = useState<any>(null);
  
  const gameTimerRef = useRef<NodeJS.Timeout | null>(null);
  const questionTimerRef = useRef<NodeJS.Timeout | null>(null);
  const questionStartTimeRef = useRef<number>(0);
  
  // Refs for storing callback functions to avoid hoisting issues
  const nextQuestionRef = useRef<(() => void) | null>(null);
  const handleQuestionTimeoutRef = useRef<(() => void) | null>(null);
  const startQuestionTimerRef = useRef<((timeLimit?: number) => void) | null>(null);
  const endGameSessionRef = useRef<((reason: 'completed' | 'time' | 'manual') => Promise<void>) | null>(null);
  
  const {
    currentSession,
    createSession,
    updateSession,
    endSession,
    gameStarted,
    gameOver,
    setLoading,
    error,
    setError,
    startGame: startLocalGame,
  } = useGameStore();

  const endGameSession = useCallback(async (reason: 'completed' | 'time' | 'manual') => {
    try {
      setIsGameActive(false);
      setCanAnswer(false);
      
      if (gameTimerRef.current) {
        clearInterval(gameTimerRef.current);
      }
      if (questionTimerRef.current) {
        clearInterval(questionTimerRef.current);
      }
      
      const messages = {
        completed: '🎊 Oyun tamamlandı! Analiz hazırlanıyor...',
        time: '⏰ Süre doldu! Analiz hazırlanıyor...',
        manual: '🛑 Oyun durduruldu! Analiz hazırlanıyor...'
      };
      
      toast.success(messages[reason], { duration: 3000 });
      
      if (currentSession?.sessionId) {
        setLoading(true);
        
        try {
          const response = await fetch(`http://localhost:8080/api/game/analysis?sessionId=${currentSession.sessionId}`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
          });
          
          if (response.ok) {
            const analysis = await response.json();
            setGameAnalysis(analysis);
            setShowAnalysis(true);
            
            toast.success('📊 Analiz hazır!', { duration: 2000 });
          } else {
            throw new Error('Analiz alınamadı');
          }
        } catch (error) {
          console.error('Analiz hatası:', error);
          toast.error('Analiz alınırken hata oluştu');
        } finally {
          setLoading(false);
        }
      }
      
      endSession();
      
    } catch (error) {
      console.error('Oyun bitirme hatası:', error);
      toast.error('Oyun bitirme sırasında bir hata oluştu');
    }
  }, [currentSession, endSession, setLoading]);

  const startQuestionTimer = useCallback((timeLimit: number = 20) => {
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    setQuestionTimeLeft(timeLimit);
    setCanAnswer(true);
    questionStartTimeRef.current = Date.now();
    
    questionTimerRef.current = setInterval(() => {
      setQuestionTimeLeft((prev) => {
        if (prev <= 1) {
          if (handleQuestionTimeoutRef.current) {
            handleQuestionTimeoutRef.current();
          }
          return 0;
        }
        
        if (prev === 5) {
          toast('⏰ 5 saniye kaldı!', { duration: 2000, icon: '⚠️' });
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
        
        if (prev === 30) {
          toast.error('⏰ Son 30 saniye!', { 
            duration: 3000,
            icon: '🚨' 
          });
        }
        
        if (prev <= 10 && prev > 0) {
          toast.error(`⏰ ${prev} saniye kaldı!`, { 
            duration: 1000 
          });
        }
        
        return prev - 1;
      });
    }, 1000);
  }, []);

  const nextQuestion = useCallback(() => {
    if (questionCount >= totalQuestions) {
      if (endGameSessionRef.current) {
        endGameSessionRef.current('completed');
      }
      return;
    }
    
    // Sonraki soruyu almak için sadece timer başlat
    // Gerçek soru alma işi FamilyRelationGame'de olacak
    const difficulty = currentSession?.difficulty ?? Difficulty.MEDIUM;
    
    let timeLimit = 15; // HARD difficulty default
    if (difficulty === Difficulty.EASY) {
      timeLimit = 20;
    } else if (difficulty === Difficulty.MEDIUM) {
      timeLimit = 18;
    }
    
    if (startQuestionTimerRef.current) {
      startQuestionTimerRef.current(timeLimit);
    }
  }, [questionCount, totalQuestions, currentSession]);

  const handleQuestionTimeout = useCallback(() => {
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    setCanAnswer(false);
    toast.error('⏰ Süre doldu! Sonraki soruya geçiliyor...', { 
      duration: 2000,
      icon: '⏳' 
    });
    
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

  const startGame = useCallback(async (playerName: string, difficulty: Difficulty) => {
    try {
      setLoading(true);
      setError(null);
      
      const initialData = await gameService.startGameSimple(playerName, difficulty);
      
      // Backend'den gelen total questions değerini kullan
      setTotalQuestions(initialData.totalQuestions);
      
      createSession(initialData.playerName, initialData.difficulty);
      
      setCurrentQuestion(initialData.firstQuestion);
      setQuestionCount(1);
      setTimeLeft(initialData.gameDurationInSeconds);
      setIsGameActive(true);
      setCanAnswer(true);
      
      startLocalGame();
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
  }, [createSession, setCurrentQuestion, setLoading, setError, startLocalGame, startGameTimer, startQuestionTimer]);

  const answerQuestion = useCallback((answer: string, isCorrect: boolean) => {
    if (!canAnswer) return;
    
    setCanAnswer(false);
    
    if (questionTimerRef.current) {
      clearInterval(questionTimerRef.current);
    }
    
    if (isCorrect) {
      const messages = [
        '🎉 Doğru!',
        '✅ Harika!',
        '🌟 Mükemmel!',
        '👏 Süper!',
        '🔥 Bravo!'
      ];
      toast.success(messages[Math.floor(Math.random() * messages.length)], {
        duration: 2000
      });
    } else {
      toast.error('❌ Yanlış cevap!', { duration: 2000 });
    }
    
    if (currentSession) {
      updateSession({
        questionsAnswered: currentSession.questionsAnswered + 1,
        correctAnswers: isCorrect ? currentSession.correctAnswers + 1 : currentSession.correctAnswers,
        currentStreak: isCorrect ? currentSession.currentStreak + 1 : 0,
        maxStreak: isCorrect ? Math.max(currentSession.maxStreak, currentSession.currentStreak + 1) : currentSession.maxStreak,
      });
    }
    
    setQuestionCount(prev => prev + 1);
    
    setTimeout(() => {
      nextQuestion();
    }, 2000);
    
  }, [canAnswer, currentSession, updateSession, nextQuestion]);

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

  const closeAnalysis = useCallback(() => {
    setShowAnalysis(false);
    setGameAnalysis(null);
  }, []);

  return {
    isGameActive,
    isPaused,
    timeLeft,
    questionTimeLeft,
    canAnswer,
    questionCount,
    totalQuestions,
    showAnalysis,
    gameAnalysis,
    currentSession,
    gameStarted,
    gameOver,
    error,
    currentQuestion,
    setCurrentQuestion,
    startGame,
    answerQuestion,
    nextQuestion,
    endGameSession,
    pauseGame,
    resumeGame,
    closeAnalysis,
    startQuestionTimer,
    getProgress: () => (questionCount / totalQuestions) * 100,
    getTimeProgress: () => (timeLeft / 180) * 100,
    getQuestionTimeProgress: () => (questionTimeLeft / 20) * 100,
    getAccuracy: () => currentSession ? 
      (currentSession.correctAnswers / Math.max(currentSession.questionsAnswered, 1)) * 100 : 0,
  };
};
