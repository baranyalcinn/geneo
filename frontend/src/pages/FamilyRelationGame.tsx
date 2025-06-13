import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Container, Typography, Card, CardContent, Box, Button, 
  FormControl, InputLabel, Select, MenuItem, TextField,
  Radio, RadioGroup, FormControlLabel, Divider,
  alpha, useTheme, Paper, TableContainer, Table, TableHead,
  TableRow, TableCell, TableBody, IconButton, Tooltip, CircularProgress,
  Chip, SelectChangeEvent, Alert, Avatar,
  useMediaQuery, styled, Grid
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import HomeIcon from '@mui/icons-material/Home';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import TimerIcon from '@mui/icons-material/Timer';
import InfoIcon from '@mui/icons-material/Info';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import Person2Icon from '@mui/icons-material/Person2';
import CakeIcon from '@mui/icons-material/Cake';
import WcIcon from '@mui/icons-material/Wc';
import WorkIcon from '@mui/icons-material/Work';
import PeopleIcon from '@mui/icons-material/People';
import LoadingIndicator from '../components/ui/LoadingIndicator';
import ErrorMessage from '../components/ui/ErrorMessage';
import RelationshipGraph from '../components/RelationshipGraph';
import { ReactFlowProvider } from '@xyflow/react';
import { useTranslation } from 'react-i18next';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbDownIcon from '@mui/icons-material/ThumbDown';
import { useGameContext } from '../contexts/GameContext';
import { toast } from 'react-hot-toast';

import { 
  Difficulty,
  GameQuestion,
  GameAnswer,
  GameResult,
  PersonInfo,
  HighScores,
  AnswerResponse,
  RelationshipStep,
  GameQuestionFeedbackDTO,
  InitialGameData
} from '../types/game';
import { 
  getQuestion, 
  submitAnswer, 
  getHighScores,
  recordGameResult,
  sendFeedback
} from '../services/gameService';

// Styled components tanımlamaları
const StyledContainer = styled(Container)(({ theme }) => ({
  minHeight: '100vh',
  display: 'flex',
  flexDirection: 'column',
  padding: theme.spacing(2),
  [theme.breakpoints.down('md')]: {
    padding: theme.spacing(1),
  }
}));

const HeaderBar = styled(Box)(({ theme }) => ({
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: theme.spacing(1, 2),
  borderRadius: theme.shape.borderRadius,
  backgroundColor: alpha(theme.palette.background.paper, 0.9),
  marginBottom: theme.spacing(2),
  boxShadow: `0 2px 8px ${alpha(theme.palette.common.black, 0.1)}`,
  backdropFilter: 'blur(10px)',
}));

const GamePlayArea = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  flexGrow: 1,
  borderRadius: Number(theme.shape.borderRadius) * 2,
  backgroundColor: alpha(theme.palette.background.paper, 0.7),
  overflow: 'auto',
  padding: theme.spacing(2),
  backdropFilter: 'blur(10px)',
  boxShadow: `0 4px 20px ${alpha(theme.palette.common.black, 0.15)}`,
  marginBottom: theme.spacing(1),
}));

const ScoreTimeDisplay = styled(Box)(({ theme }) => ({
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  flexWrap: 'wrap',
  gap: theme.spacing(1),
  marginBottom: theme.spacing(1),
}));

const QuestionCard = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  backgroundColor: alpha(theme.palette.background.paper, 0.95),
  borderRadius: Number(theme.shape.borderRadius) * 1.5,
  boxShadow: `0 4px 12px ${alpha(theme.palette.primary.main, 0.15)}`,
  border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
}));

const GameCard = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  backgroundColor: theme.palette.background.paper,
  borderRadius: Number(theme.shape.borderRadius) * 1.5,
  boxShadow: `0 2px 10px ${alpha(theme.palette.common.black, 0.1)}`,
}));

const OptionButton = styled(FormControlLabel, {
  shouldForwardProp: (prop) => prop !== 'isSelected'
})<{ isSelected?: boolean }>(({ theme, isSelected }) => ({
  width: '100%',
  margin: theme.spacing(0.5, 0),
  padding: theme.spacing(1),
  borderRadius: theme.shape.borderRadius,
  border: `2px solid ${isSelected ? theme.palette.primary.main : theme.palette.divider}`,
  backgroundColor: isSelected ? alpha(theme.palette.primary.main, 0.1) : 'transparent',
  transition: 'all 0.2s ease',
  cursor: 'pointer',
  '&:hover': {
    backgroundColor: isSelected ? alpha(theme.palette.primary.main, 0.15) : alpha(theme.palette.primary.main, 0.05),
    transform: 'translateY(-1px)',
    boxShadow: `0 2px 8px ${alpha(theme.palette.common.black, 0.08)}`,
  },
}));

const AnimatedButton = styled(Button)(({ theme }) => ({
  transition: 'all 0.2s ease-in-out',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: `0 4px 12px ${alpha(theme.palette.primary.main, 0.25)}`,
  },
}));

interface PersonInfoDisplayProps {
  personName: string;
  personInfo: PersonInfo;
  isTarget?: boolean;
}

const PersonInfoDisplay = ({ personName, personInfo, isTarget = false }: PersonInfoDisplayProps) => {
  const theme = useTheme();
  const { t } = useTranslation();
  
  // Cinsiyet anahtarını güvenli bir şekilde oluştur
  const genderKey = personInfo?.gender ? `gender.${personInfo.gender.toLowerCase()}` : '';

  return (
    <GameCard sx={{
      borderColor: isTarget ? theme.palette.secondary.main : theme.palette.primary.main,
      borderWidth: 1,
      borderStyle: 'solid',
      boxShadow: isTarget 
        ? `0 4px 12px ${alpha(theme.palette.secondary.main, 0.2)}`
        : `0 4px 12px ${alpha(theme.palette.primary.main, 0.2)}`,
    }}>
      <Typography 
        variant="subtitle1" 
        fontWeight="medium" 
        sx={{ 
          mb: 0.75, 
          color: isTarget ? theme.palette.secondary.main : theme.palette.primary.main,
          borderBottom: `1px solid ${alpha(isTarget ? theme.palette.secondary.main : theme.palette.primary.main, 0.2)}`,
          pb: 0.5
        }}
      >
        {personName}
      </Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {personInfo?.gender && (
          <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <WcIcon fontSize="small" color="action" />
            {t(genderKey as any, { defaultValue: personInfo.gender })}
          </Typography>
        )}
        {personInfo?.birthYear && (
          <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <CakeIcon fontSize="small" color="action" />
            {t('birth_year')}: {personInfo.birthYear}
          </Typography>
        )}
      </Box>
    </GameCard>
  );
};

// GameAnswer tipini uyumlu hale getir
type ExtendedGameAnswer = Partial<GameAnswer> & {
  playerName: string;
  difficulty: Difficulty;
  currentScore: number;
  currentStreak: number;
  timeTakenInSeconds: number;
  questionId: string;
  answer: string;
};

const FamilyRelationGame = () => {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const navigate = useNavigate();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  // Game context
  const {
    timeLeft,
    questionTimeLeft, 
    isGameActive,
    canAnswer,
    questionCount,
    totalQuestions,
    currentQuestion,
    setCurrentQuestion,
    playerName,
    setPlayerName,
    currentDifficulty,
    setCurrentDifficulty,
    gameStarted,
    setGameStarted,
    gameOver,
    setGameOver,
    currentScore,
    currentStreak,
    maxStreak,
    correctAnswers,
    lastAnswerResponse,
    setLastAnswerResponse,
    resetGame,
    addScore,
    incrementCorrectAnswers,
    updateStreak,
    setIsGameActive,
    setCanAnswer,
    setTimeLeft,
    setQuestionTimeLeft,
    setQuestionCount
  } = useGameContext();

  // Local state with performance optimization
  const [selectedAnswer, setSelectedAnswer] = useState<string>('');
  const [showResult, setShowResult] = useState<boolean>(false);
  const [isCorrect, setIsCorrect] = useState<boolean>(false);
  const [correctAnswerText, setCorrectAnswerText] = useState<string>('');
  const [showHint, setShowHint] = useState<boolean>(false);
  const [showPath, setShowPath] = useState<boolean>(false);
  const [finalResult, setFinalResult] = useState<GameResult | null>(null);
  const [highScores, setHighScores] = useState<HighScores>({
    [Difficulty.EASY]: [],
    [Difficulty.MEDIUM]: [],
    [Difficulty.HARD]: [],
  });
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [showScores, setShowScores] = useState<boolean>(false);
  const [feedbackSent, setFeedbackSent] = useState<boolean>(false);

  // Performance refs
  const questionStartTime = useRef<number>(0);
  const gameInitialized = useRef<boolean>(false);
  const lastQuestionId = useRef<string>('');
  const gameAreaRef = useRef<HTMLDivElement>(null);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  
  // Memoized callbacks for performance
  const resetTimer = useCallback(() => {
    questionStartTime.current = Date.now();
  }, []);

  const formatTime = useCallback((seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }, []);

  // Memoized values for performance
  const gameProgress = useMemo(() => 
    Math.round((questionCount / totalQuestions) * 100), 
    [questionCount, totalQuestions]
  );

  const timeDisplays = useMemo(() => ({
    gameTime: formatTime(timeLeft),
    questionTime: formatTime(questionTimeLeft),
    isGameTimeCritical: timeLeft <= 30,
    isQuestionTimeCritical: questionTimeLeft <= 5
  }), [timeLeft, questionTimeLeft, formatTime]);

  const gameStats = useMemo(() => ({
    accuracy: questionCount > 0 ? Math.round((correctAnswers / questionCount) * 100) : 0,
    questionsRemaining: totalQuestions - questionCount,
    averageTimePerQuestion: questionCount > 0 ? (180 - timeLeft) / questionCount : 0
  }), [questionCount, correctAnswers, totalQuestions, timeLeft]);

  const getPersonInfoFromName = useCallback((personNameKey: string, path?: RelationshipStep[]): PersonInfo => {
    if (!currentQuestion) {
      return { id: '', fullName: personNameKey };
    }

    // Önce path'ten ara
    if (path) {
      const foundInPath = path.find(step => 
        step.personName === personNameKey || 
        step.personName.includes(personNameKey)
      );
      if (foundInPath) {
        return {
          id: foundInPath.personId,
          fullName: foundInPath.personName,
          gender: foundInPath.personGender,
          birthYear: foundInPath.personBirthYear,
          deathYear: foundInPath.personDeathYear
        };
      }
    }

    // Person1 ve Person2 bilgilerini kontrol et
    const { person1Info, person2Info } = currentQuestion;
    
    if (person1Info?.fullName === personNameKey) {
      return person1Info;
    }
    if (person2Info?.fullName === personNameKey) {
      return person2Info;
    }

    // Default return
    return { id: '', fullName: personNameKey };
  }, [currentQuestion]);

  const fetchHighScores = useCallback(async () => {
    try {
      const scoresFromApi = await getHighScores(); 
      setHighScores(scoresFromApi); 
    } catch (err) {
      console.error('Yüksek skorlar getirilirken hata oluştu:', err);
      setHighScores({
        [Difficulty.EASY]: [],
        [Difficulty.MEDIUM]: [],
        [Difficulty.HARD]: [],
      });
    }
  }, []);

  const handleGameOver = useCallback(async (finalResultParam?: GameResult | null) => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    // Save final score to local state
    const finalScore = currentScore;
    const finalCorrectAnswers = correctAnswers;
    const finalTotalQuestions = totalQuestions;
    const finalMaxStreak = maxStreak;

    const gameResult: GameResult = {
      playerName: playerName,
      score: finalScore,
      difficulty: currentDifficulty,
      correctAnswers: finalCorrectAnswers,
      totalQuestions: finalTotalQuestions,
      maxStreak: finalMaxStreak,
      accuracy: (finalCorrectAnswers / Math.max(finalTotalQuestions, 1)) * 100
    };
    
    setFinalResult(gameResult);
    await fetchHighScores();
  }, [currentScore, correctAnswers, totalQuestions, maxStreak, playerName, currentDifficulty, fetchHighScores]);

  const handleNextQuestionClick = useCallback(async () => {
    if (!canAnswer || !isGameActive || gameOver) return;
    
    try {
      const nextQuestionData = await getQuestion(currentDifficulty, 'tr');
      if (nextQuestionData) {
        // Component'ın mount edildiğini kontrol et
        if (gameInitialized.current) {
          setCurrentQuestion(nextQuestionData);
          setShowResult(false);
          setSelectedAnswer('');
          setLastAnswerResponse(null);
          setFeedbackSent(false);
          resetTimer();
        }
      } else {
        console.warn('Sonraki soru alınamadı, oyun bitiyor...');
        if (gameInitialized.current) {
          handleGameOver(null);
        }
      }
    } catch (error) {
      console.error('Sonraki soru yüklenemedi:', error);
      if (gameInitialized.current) {
        setError('Sonraki soru yüklenemedi. Oyun bitiyor...');
        setTimeout(() => handleGameOver(null), 2000);
      }
    }
  }, [canAnswer, isGameActive, gameOver, currentDifficulty, resetTimer, setCurrentQuestion, setLastAnswerResponse, handleGameOver]);

  const formatDifficulty = useCallback((diff: string): string => {
    const difficultyMap = { 'EASY': 'Kolay', 'MEDIUM': 'Orta', 'HARD': 'Zor' };
    return difficultyMap[diff as keyof typeof difficultyMap] || diff;
  }, []);

  const toggleShowPath = useCallback(() => {
    setShowPath(prev => !prev);
  }, []);

  const toggleHint = useCallback(() => {
    setShowHint(prev => !prev);
  }, []);

  const handleFeedback = useCallback(async (feedback: 'good' | 'bad') => {
    if (!currentQuestion) return;
    
    try {
      const feedbackData: GameQuestionFeedbackDTO = {
        questionId: currentQuestion.id,
        relationshipType: currentQuestion.relationshipType,
        isCorrect: isCorrect,
        feedback: feedback
      };
      
      await sendFeedback(feedbackData);
      setFeedbackSent(true);
      toast.success(feedback === 'good' ? '👍 Teşekkürler!' : '👎 Geri bildiriminiz alındı');
    } catch (error) {
      console.error('Geri bildirim gönderme hatası:', error);
    }
  }, [currentQuestion, isCorrect]);

  const restartGame = useCallback(() => {
    setShowResult(false);
    setSelectedAnswer('');
    setShowScores(false);
    setError(null);
    setShowHint(false);
    setShowPath(false);
    setLastAnswerResponse(null);
    setFeedbackSent(false);
    setFinalResult(null);
    
    resetGame();
  }, [resetGame, setLastAnswerResponse]);

  const startGame = useCallback(async () => {
    if (!playerName.trim() || isLoading) return;
    
    setIsLoading(true);
    setGameStarted(true);
    setIsGameActive(true);
    setError(null);
    
    try {
      // İlk soruyu al
      const firstQuestion = await getQuestion(currentDifficulty, 'tr');
      
      if (firstQuestion) {
        setCurrentQuestion(firstQuestion);
        setGameStarted(true);
        setIsGameActive(true);
        resetTimer();
        toast.success(`🎮 ${formatDifficulty(currentDifficulty.toString())} seviyesinde oyun başladı!`);
      } else {
        throw new Error('İlk soru yüklenemedi');
      }
    } catch (error: any) {
      console.error('Oyun başlatma hatası:', error);
      
      // Detaylı hata mesajları
      let errorMessage = 'Oyun başlatılamadı. ';
      
      if (error.message.includes('Backend sunucusuna bağlanılamıyor')) {
        errorMessage += 'Backend sunucusu çalışmıyor olabilir. PowerShell\'de "cd backend; mvn spring-boot:run" komutu ile sunucuyu başlatın.';
      } else if (error.message.includes('Soru servisi bulunamadı')) {
        errorMessage += 'API endpoint\'leri kontrol edin.';
      } else if (error.message.includes('Sunucu hatası')) {
        errorMessage += 'Sunucu tarafında bir problem var. Lütfen daha sonra tekrar deneyin.';
      } else {
        errorMessage += error.message || 'Bilinmeyen bir hata oluştu.';
      }
      
      setError(errorMessage);
      toast.error('Oyun başlatılamadı - Backend kontrol edin');
      setGameStarted(false);
      setIsGameActive(false);
    } finally {
      setIsLoading(false);
    }
  }, [playerName, currentDifficulty, isLoading, resetTimer, setGameStarted, setIsGameActive, setCurrentQuestion]);

  const checkAnswer = useCallback(async (answer: string) => {
    if (!canAnswer || !currentQuestion || isLoading) return;
    
    const timeTaken = (Date.now() - questionStartTime.current) / 1000;
    setIsLoading(true);
    
    try {
      const answerDetails: ExtendedGameAnswer = {
        questionId: currentQuestion.id,
        answer: answer,
        timeTakenInSeconds: timeTaken,
        difficulty: currentDifficulty,
        playerName: playerName,
        currentScore: currentScore,
        currentStreak: currentStreak,
        questionsAnswered: questionCount,
        correctAnswersCount: correctAnswers
      };

      const response = await submitAnswer(answerDetails, 'tr');
      
      // Component mount kontrolü
      if (!gameInitialized.current) return;

      setLastAnswerResponse(response);
      setIsCorrect(response.correctAnswer);
      setCorrectAnswerText(response.correctAnswerText || '');
      setShowResult(true);

      // Toast notification with performance optimization
      if (response.correctAnswer) {
        const messages = [
          '🎉 Doğru!',
          '✅ Harika!',
          '🌟 Mükemmel!',
          '👏 Süper!',
          '🔥 Bravo!'
        ];
        toast.success(`${messages[Math.floor(Math.random() * messages.length)]} +${response.pointsEarned} puan`, { 
          duration: 2000,
          position: 'top-center'
        });
      } else {
        toast.error(`❌ Yanlış! Doğru cevap: ${response.correctAnswerText}`, { 
          duration: 3000,
          position: 'top-center'
        });
      }

      // Sonraki soruyu ayarla
      if (response.nextQuestion && !response.gameOver) {
        setCurrentQuestion(response.nextQuestion);
        resetTimer();
      }

      // Oyun bitiş kontrolü
      if (response.gameOver) {
        setTimeout(() => {
          if (gameInitialized.current) {
            handleGameOver(response.finalResult);
          }
        }, 2000);
      }

    } catch (error: any) {
      console.error('Cevap gönderme hatası:', error);
      if (gameInitialized.current) {
        setError(error.message || 'Cevap gönderilemedi');
        toast.error('Cevap gönderilemedi');
      }
    } finally {
      if (gameInitialized.current) {
        setIsLoading(false);
      }
    }
  }, [canAnswer, currentQuestion, isLoading, currentDifficulty, playerName, 
      currentScore, currentStreak, questionCount, correctAnswers]);





  // Memoized hint calculation
  const hintData = useMemo(() => {
    const getHintByDifficulty = (): { generalHint: string, specificHints: string[] } => {
      switch (currentDifficulty) {
        case Difficulty.EASY:
          return {
            generalHint: "Bu kolay seviye bir soru. Doğrudan aile bağlarını düşünün.",
            specificHints: [
              "Anne, baba, kardeş gibi yakın aile üyelerini kontrol edin",
              "Cinsiyete dikkat edin (erkek/kadın)",
              "Yaş farkını göz önünde bulundurun"
            ]
          };
        case Difficulty.MEDIUM:
          return {
            generalHint: "Bu orta seviye bir soru. Teyze, amca, kuzen gibi uzak akrabaları düşünün.",
            specificHints: [
              "Anne veya baba tarafından aile bağlarını inceleyin",
              "Teyze (anne kız kardeşi), Dayı (anne erkek kardeşi)",
              "Amca (baba erkek kardeşi), Hala (baba kız kardeşi)",
              "Kuzen ilişkilerini kontrol edin"
            ]
          };
        case Difficulty.HARD:
          return {
            generalHint: "Bu zor seviye bir soru. Karmaşık aile bağlarını ve evlilik ilişkilerini düşünün.",
            specificHints: [
              "Kayın ailesi ilişkilerini göz önünde bulundurun",
              "Gelin, damat, kaynana, kaynata gibi evlilik bağları",
              "Çok adımlı aile bağlarını takip edin",
              "Nesil farkları ve yaş uyumluluğunu kontrol edin"
            ]
          };
        default:
          return {
            generalHint: "Aile ilişkilerini dikkatlice düşünün.",
            specificHints: ["Türk aile yapısındaki geleneksel isimlendirmeleri hatırlayın"]
          };
      }
    };
    
    return getHintByDifficulty();
  }, [currentDifficulty]);

  // Component mount/unmount optimization
  useEffect(() => {
    gameInitialized.current = true;
    
    return () => {
      gameInitialized.current = false;
    };
  }, []);

  // High scores loading optimization
  useEffect(() => {
    let mounted = true;
    
    const loadHighScores = async () => {
      try {
        const scores = await getHighScores();
        if (mounted && scores) {
          setHighScores(scores);
        }
      } catch (error) {
        console.error('High scores yüklenemedi:', error);
      }
    };

    loadHighScores();
    
    return () => {
      mounted = false;
    };
  }, []);

  // Question change optimization
  useEffect(() => {
    if (currentQuestion && currentQuestion.id !== lastQuestionId.current) {
      lastQuestionId.current = currentQuestion.id;
      resetTimer();
      setSelectedAnswer('');
      setShowResult(false);
      setShowHint(false);
      setShowPath(false);
    }
  }, [currentQuestion, resetTimer]);

  const relationshipPathForGraph = useMemo(() => {
    console.log('🔍 Debug relationshipPathForGraph:', {
      showResult,
      currentRelationshipPath: currentQuestion?.relationshipPath?.length || 0,
      currentQuestion: currentQuestion?.id
    });
    
    // Eğer sunucudan gelen detaylı bir yol varsa (cevap sonrası) onu kullan
    if (showResult && currentQuestion && currentQuestion.relationshipPath && currentQuestion.relationshipPath.length > 0) {
      console.log('✅ Using question relationshipPath:', currentQuestion.relationshipPath);
      return currentQuestion.relationshipPath;
    }

    // Eğer soru bilgisi yoksa, boş bir grafik göster
    if (!currentQuestion || !currentQuestion.person1Info || !currentQuestion.person2Info) {
      return undefined;
    }

    // Cevap gösterilmiyorsa veya cevap sonrası yol gelmediyse, temel bir grafik oluştur
    const { person1Info, person2Info } = currentQuestion;
    const relationshipLabel = showResult ? (lastAnswerResponse?.correctAnswerText || '...') : `?`;

    const basicPath = [
      {
        personId: String(person1Info.id),
        personName: person1Info.fullName,
        personGender: person1Info.gender,
        personBirthYear: person1Info.birthYear || undefined,
        relationshipToNextPerson: relationshipLabel,
        sourcePerson: true,
        targetPerson: false,
      },
      {
        personId: String(person2Info.id),
        personName: person2Info.fullName,
        personGender: person2Info.gender,
        personBirthYear: person2Info.birthYear || undefined,
        relationshipToNextPerson: undefined, // Son kişi için undefined
        sourcePerson: false,
        targetPerson: true,
      }
    ];

    return basicPath;
  }, [currentQuestion, showResult, lastAnswerResponse]);

  // Layout yönünü otomatik seç: 3+ kişi için LR, az kişi için TB
  const optimalLayoutDirection = useMemo(() => {
    const pathLength = relationshipPathForGraph?.length || 0;
    return pathLength >= 3 ? 'LR' : 'TB';
  }, [relationshipPathForGraph]);

  const person1InfoToDisplay = currentQuestion?.person1Info || null;
  const person2InfoToDisplay = currentQuestion?.person2Info || null;
  const person1 = currentQuestion?.person1Info?.fullName || '...';
  const person2 = currentQuestion?.person2Info?.fullName || '...';

  // Component functions
  const fetchNextQuestion = useCallback(async () => {
    try {
      setIsLoading(true);
      const nextQuestionData = await getQuestion(currentDifficulty, i18n.language);
      
      if (nextQuestionData) {
        setCurrentQuestion(nextQuestionData);
        setShowResult(false);
        setSelectedAnswer('');
        setIsCorrect(false);
        setLastAnswerResponse(null);
        setFeedbackSent(false);
      } else {
        setError('Sonraki soru alınamadı');
      }
    } catch (error: any) {
      console.error('Sonraki soru alınamadı:', error);
      setError(error.message || 'Sonraki soru yüklenemedi');
    } finally {
      setIsLoading(false);
    }
  }, [currentDifficulty, i18n.language, setCurrentQuestion, setLastAnswerResponse]);

  // Duplicate functions removed - using useCallback versions above

  const getHintByDifficulty = (): { generalHint: string, specificHints: string[] } => {
    if (!currentQuestion) return { generalHint: "", specificHints: [] };
    
    const difficultyKey = currentQuestion.difficulty.toLowerCase();
    
    return {
      generalHint: t(`hints.${difficultyKey}.general`),
      specificHints: t(`hints.${difficultyKey}.specific`, { returnObjects: true }) as string[]
    };
  };

  const renderHintSection = () => {
    if (!currentQuestion || !showHint) return null;
    const hintData = getHintByDifficulty();
    return (
      <GameCard sx={{ mt: 2, background: alpha(theme.palette.info.light, 0.05) }}>
        <Typography variant="h6" gutterBottom sx={{ color: theme.palette.info.main, display: 'flex', alignItems: 'center' }}>
          <HelpOutlineIcon sx={{ mr: 1 }} /> {t('hint.title')}
        </Typography>
        <Typography variant="body2" sx={{ mb: 1 }}>{hintData.generalHint}</Typography>
        {Array.isArray(hintData.specificHints) && hintData.specificHints.length > 0 && (
          <Box>
            {hintData.specificHints.map((hint, index) => (
              <Chip key={index} label={hint} size="small" sx={{ mr: 0.5, mb: 0.5, background: alpha(theme.palette.info.main, 0.1) }} />
            ))}
          </Box>
        )}
      </GameCard>
    );
  };

  const renderGameSetup = () => (
      <GamePlayArea>
        <Box 
          sx={{ 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center', 
            justifyContent: 'center', 
            flexGrow: 1,
            padding: { xs: 2, md: 4 },
            gap: 4,
          }}
        >
          <Paper 
            elevation={3}
            sx={{
              p: { xs: 2, sm: 3 },
              borderRadius: Number(theme.shape.borderRadius) * 2,
              width: '100%',
              maxWidth: 500,
              textAlign: 'center',
              background: alpha(theme.palette.background.paper, 0.9),
              backdropFilter: 'blur(15px)',
            }}
          >
            <Box sx={{ mb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <Avatar 
                sx={{ 
                  width: 80, 
                  height: 80, 
                  bgcolor: alpha(theme.palette.primary.main, 0.15), 
                  color: theme.palette.primary.main,
                  mb: 2,
                }}
              >
                <SportsEsportsIcon sx={{ fontSize: 40 }} />
              </Avatar>
              <Typography variant="h4" component="h1" gutterBottom fontWeight="bold" color="primary.main">
                {t('game.title')}
              </Typography>
              <Typography variant="subtitle1" sx={{ mb: 1, color: 'text.secondary' }}>
                {t('game.subtitle')}
              </Typography>
              <Divider sx={{ width: '50%', my: 2 }} />
            </Box>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
                <TextField
                  fullWidth
                  label={t('player_name.label')}
                  variant="outlined"
                  value={playerName}
                  onChange={(e) => setPlayerName(e.target.value)}
                  size="small"
                  placeholder={t('player_name.placeholder')}
                  InputProps={{
                    startAdornment: <Person2Icon fontSize="small" sx={{ mr: 0.75, color: theme.palette.action.active }} />,
                  }}
                />
                <FormControl fullWidth variant="outlined" size="small">
                  <InputLabel>{t('difficulty.label')}</InputLabel>
                  <Select
                    value={currentDifficulty}
                    onChange={(e) => setCurrentDifficulty(e.target.value as Difficulty)}
                    label={t('difficulty.label')}
                  >
                    <MenuItem value={Difficulty.EASY}>{t('difficulty.easy')}</MenuItem>
                    <MenuItem value={Difficulty.MEDIUM}>{t('difficulty.medium')}</MenuItem>
                    <MenuItem value={Difficulty.HARD}>{t('difficulty.hard')}</MenuItem>
                  </Select>
                </FormControl>
                <FormControl fullWidth variant="outlined" size="small">
                  <InputLabel>Dil / Language</InputLabel>
                  <Select
                    value={i18n.language}
                    onChange={(e) => handleLanguageChange(e.target.value)}
                    label="Dil / Language"
                  >
                    <MenuItem value="tr">🇹🇷 Türkçe</MenuItem>
                    <MenuItem value="en">🇺🇸 English</MenuItem>
                  </Select>
                </FormControl>
            </Box>

            <Button
              variant="contained"
              color="primary"
              size="large"
              startIcon={<SportsEsportsIcon />}
              onClick={startGame}
              disabled={isLoading || !playerName.trim()}
            >
              {t('start_game')}
            </Button>
            
            <Button
              variant="text"
              size="small"
              color="secondary"
              startIcon={<LeaderboardIcon />}
              onClick={() => setShowScores(true)}
              sx={{ mt: 2 }}
            >
              {t('high_scores.title')}
            </Button>
          </Paper>
        </Box>
      </GamePlayArea>
  );

  const renderGameOver = () => {
    const final = finalResult || lastAnswerResponse?.finalResult;
    if(!final) return null;

    const accuracy = final.totalQuestions > 0 ? (final.correctAnswers / final.totalQuestions) * 100 : 0;
    let performanceMessage = "";
    if (accuracy >= 80) performanceMessage = t('performance.excellent');
    else if (accuracy >= 60) performanceMessage = t('performance.good');
    else if (accuracy >= 40) performanceMessage = t('performance.average');
    else performanceMessage = t('performance.poor');

    return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', p: isMobile ? 2 : 4, textAlign: 'center' }}>
      <GameCard sx={{ maxWidth: 600, width: '100%' }}>
        <Typography variant={isMobile? "h5" : "h4"} component="h2" gutterBottom sx={{ color: theme.palette.primary.main, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <EmojiEventsIcon sx={{ fontSize: isMobile ? 30 : 40, mr: 1.5, color: theme.palette.warning.main }} /> {t('game_over.title')}
        </Typography>
        <Typography variant="h6" sx={{ mb: 1 }}>{playerName}, {t('game_over.score')}: {final.score}</Typography>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', mb: 2 }}>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.total_questions')}: {final.totalQuestions}</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.correct_answers')}: {final.correctAnswers}</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.accuracy')}: {accuracy.toFixed(1)}%</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.max_streak')}: {final.maxStreak}</Typography>
          </Box>
        </Box>
        
        <Typography variant="subtitle1" sx={{ mb: 3, fontStyle: 'italic' }}>{performanceMessage}</Typography>
        
        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, flexWrap: 'wrap' }}>
          <AnimatedButton 
            variant="contained" 
            color="primary" 
            onClick={restartGame}
            startIcon={<RefreshIcon />}
            size="large"
          >
            {t('restart_game')}
          </AnimatedButton>
          <AnimatedButton 
            variant="outlined" 
            color="secondary" 
            onClick={() => setShowScores(true)}
            startIcon={<LeaderboardIcon />}
            size="large"
          >
            {t('high_scores.title')}
          </AnimatedButton>
          <AnimatedButton 
            variant="outlined" 
            onClick={() => navigate('/')}
            startIcon={<HomeIcon />}
            size="large"
          >
            {t('home')}
          </AnimatedButton>
        </Box>
      </GameCard>
    </Box>
  )};

  const setCurrentQuestionSafely = (questionData: GameQuestion | null) => {
    if (!questionData) {
      setCurrentQuestion(null);
    } else {
      const safeQuestion = {
        ...questionData,
        options: questionData.options || [],
      };
      setCurrentQuestion(safeQuestion);
    }
  };

  const renderHighScores = () => {
    if (!highScores) return <Typography>{t('high_scores.loading')}</Typography>;
    
    const allScores = Object.values(highScores).flat();
    
    const topScores = allScores
        .sort((a, b) => b.score - a.score)
        .slice(0, 10);
    
    if (topScores.length === 0) return <Typography>{t('high_scores.no_scores')}</Typography>;
    
    return (
      <TableContainer component={Paper} elevation={0} variant="outlined">
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell>{t('high_scores.rank')}</TableCell>
              <TableCell>{t('high_scores.player')}</TableCell>
              <TableCell>{t('high_scores.difficulty')}</TableCell>
              <TableCell align="right">{t('high_scores.score')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {topScores.map((s, i) => (
              <TableRow key={s.id || i} hover>
                <TableCell component="th" scope="row">{i+1}</TableCell>
                <TableCell>{s.playerName}</TableCell>
                <TableCell>{s.difficulty ? formatDifficulty(s.difficulty) : '-'}</TableCell>
                <TableCell align="right" sx={{fontWeight:'bold'}}>{s.score}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    );
  };

  const handleLanguageChange = (lang: string) => {
    i18n.changeLanguage(lang);
    if (gameStarted && currentQuestion) {
      fetchNextQuestion();
    }
  };
  
  useEffect(() => {
    fetchHighScores();
  }, [fetchHighScores]);

  // Cevap sonrası state güncellemelerini ayrı useEffect'te yap
  useEffect(() => {
    if (lastAnswerResponse && selectedAnswer) {
      // Cevabı işle
      if (lastAnswerResponse.correctAnswer) {
        incrementCorrectAnswers();
        addScore(lastAnswerResponse.pointsEarned);
        updateStreak(true);
      } else {
        updateStreak(false);
      }
      
      // Sonraki soruyu ayarla
      if (lastAnswerResponse.nextQuestion) {
        setCurrentQuestion(lastAnswerResponse.nextQuestion);
      }
      
      // Eğer oyun bittiyse final result'ı ayarla
      if (lastAnswerResponse.gameOver) {
        console.log("Oyun bitti, final result ayarlanıyor");
        setFinalResult({
          playerName: playerName,
          score: lastAnswerResponse.updatedScore,
          difficulty: currentDifficulty,
          correctAnswers: correctAnswers + (lastAnswerResponse.correctAnswer ? 1 : 0),
          totalQuestions: totalQuestions,
          maxStreak: maxStreak,
          date: new Date().toISOString().split('T')[0]
        });
        
        // Oyun bittiğinde session'ı temizle
        setTimeout(() => {
          // Oyunu bitir ve sonucu kaydet
          setGameOver(true);
          setIsGameActive(false);
        }, 3000);
      }
    }
  }, [lastAnswerResponse, selectedAnswer]);

  useEffect(() => {
    if (!gameStarted) {
      const lastPlayerName = localStorage.getItem('playerName');
      if (lastPlayerName) {
        setPlayerName(lastPlayerName);
      }
    }
  }, [gameStarted]);

  if (!gameStarted) {
    return renderGameSetup();
  }

  if (gameOver) {
    return renderGameOver();
  }

  if (isLoading && !currentQuestion) {
    return <LoadingIndicator />;
  }

  if (error && !currentQuestion) {
    return <ErrorMessage message={error} />;
  }
  
  if (!currentQuestion) {
    return <ErrorMessage message={t('error.question_load')} />;
  }

  const { options, timeLimit, difficulty: questionDifficulty } = currentQuestion;
  const timeProgress = (timeLimit || 30) > 0 ? (questionTimeLeft / (timeLimit || 30)) * 100 : 0;

  return (
    <StyledContainer maxWidth={false} disableGutters>
      <HeaderBar>
        <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center' }}>
          <SportsEsportsIcon sx={{ mr: 1 }} /> {t('game.title')}
        </Typography>
        <Box>
          <Tooltip title={t('restart_game')}>
            <IconButton onClick={restartGame}><RefreshIcon /></IconButton>
          </Tooltip>
          <Tooltip title={t('home')}>
            <IconButton onClick={() => navigate('/')}><HomeIcon /></IconButton>
          </Tooltip>
        </Box>
      </HeaderBar>
      <GamePlayArea ref={gameAreaRef} tabIndex={-1}>
        <Box sx={{ 
          display: 'flex', 
          flexDirection: { xs: 'column', md: 'row' }, 
          flexGrow: 1, 
          gap: { xs: 1.5, md: 2 },
          height: '100%'
        }}>
          <Box sx={{ 
            flex: { xs: '1 1 auto', md: '0 0 45%' }, 
            display: 'flex', 
            flexDirection: 'column', 
            gap: { xs: 1.5, md: 2 }, 
            height: { xs: 'auto', md: '100%' }, 
            overflowY: 'auto', 
            pr: { xs: 0, md: 1 } 
          }}>
            <ScoreTimeDisplay>
              <Chip 
                icon={<EmojiEventsIcon />} 
                label={`${t('game.score')}: ${currentScore || 0}`} 
                color="primary" 
                variant="filled" 
              />
              <Chip 
                icon={<TimerIcon />} 
                label={`${t('streak')}: ${currentStreak || 0}`} 
                color="secondary" 
                variant="filled" 
              />
            </ScoreTimeDisplay>

            {timeLimit && timeLimit > 0 && (
              <Box sx={{ width: '100%', mb: 1 }}>
                <Box sx={{ height: 6, backgroundColor: alpha(theme.palette.secondary.light, 0.3), borderRadius: 3, overflow: 'hidden' }}>
                  <Box sx={{ 
                    width: `${timeProgress}%`, 
                    height: '100%', 
                    backgroundColor: questionTimeLeft < 10 ? theme.palette.error.main : questionTimeLeft < 20 ? theme.palette.warning.main : theme.palette.secondary.main, 
                    borderRadius: 3, 
                    transition: 'width 1s linear, background-color 0.5s ease' 
                  }} />
                </Box>
              </Box>
            )}
            
            <QuestionCard elevation={1}>
              <Typography 
                variant="h6" 
                component="h2"
                sx={{ mb: 1, fontWeight: '600' }}
              >
                {t('game.question_title', { number: questionCount })}
              </Typography>
              <Typography 
                variant="body1" 
                sx={{ mb: 1.5, fontWeight: "500" }}
              >
                {currentQuestion.questionText || t('game.question', { person1: person1, person2: person2 })}
              </Typography>
            </QuestionCard>

            <GameCard elevation={2}>
              <Typography 
                variant="subtitle1" 
                fontWeight="600" 
                sx={{ mb: 1, pb: 0.5, borderBottom: `1px solid ${theme.palette.divider}`}}
              >
                {t('answer_options')}:
              </Typography>
              <RadioGroup
                value={selectedAnswer}
                onChange={(e) => setSelectedAnswer(e.target.value)}
              >
                {options.map((option: string, index: number) => (
                  <OptionButton
                    key={`${option}-${index}`}
                    value={option}
                    isSelected={selectedAnswer === option}
                    control={<Radio sx={{display: 'none'}} />}
                    label={
                      <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                        <Typography variant="body1" sx={{ flexGrow: 1, fontWeight: selectedAnswer === option ? 600 : 400 }}>
                          {option}
                        </Typography>
                        {showResult && selectedAnswer === option && (
                          isCorrect ? <CheckCircleIcon color="success" /> : <CancelIcon color="error" />
                        )}
                        {showResult && lastAnswerResponse?.correctAnswerText === option && selectedAnswer !== option && (
                          <CheckCircleIcon color="disabled" />
                        )}
                      </Box>
                    }
                    disabled={showResult || isLoading}
                    sx={{
                      ...(showResult && lastAnswerResponse?.correctAnswerText === option && {
                        borderColor: `${theme.palette.success.main} !important`,
                        backgroundColor: `${alpha(theme.palette.success.light, 0.2)} !important`,
                      }),
                      ...(showResult && selectedAnswer === option && !isCorrect && {
                        borderColor: `${theme.palette.error.main} !important`,
                        backgroundColor: `${alpha(theme.palette.error.light, 0.2)} !important`,
                      }),
                    }}
                    onClick={() => !showResult && !isLoading && setSelectedAnswer(option)}
                  />
                ))}
              </RadioGroup>
              <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <AnimatedButton
                  variant="contained"
                  onClick={() => checkAnswer(selectedAnswer)}
                  disabled={!selectedAnswer || showResult || isLoading}
                  startIcon={isLoading ? <CircularProgress size={20} color="inherit" /> : <CheckCircleIcon />}
                >
                  {isLoading ? t('checking_answer') : t('game.check_answer')}
                </AnimatedButton>
                <Tooltip title={showHint ? t('hide_hint') : t('show_hint')}>
                  <IconButton onClick={toggleHint} color="info" >
                    <HelpOutlineIcon />
                  </IconButton>
                </Tooltip>
              </Box>

              {showResult && lastAnswerResponse && (
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <Alert 
                    severity={isCorrect ? "success" : "error"}
                    iconMapping={{
                      success: <CheckCircleIcon fontSize="inherit" />,
                      error: <CancelIcon fontSize="inherit" />,
                    }}
                    sx={{ justifyContent: 'center' }}
                  >
                    <Typography variant="h6">
                      {isCorrect ? t('game.correct_answer') : t('game.wrong_answer')}
                    </Typography>
                    {!isCorrect && <Typography>{t('game.correct_is', { answer: lastAnswerResponse.correctAnswerText })}</Typography>}
                    <Typography>{t('game.points_earned', { points: lastAnswerResponse.pointsEarned })}</Typography>
                  </Alert>
                  
                  {!feedbackSent ? (
                      <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2" color="text.secondary">{t('game.feedback_prompt')}</Typography>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Tooltip title={t('game.feedback_good') || ''}>
                            <IconButton onClick={() => handleFeedback('good')} color="success">
                              <ThumbUpIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title={t('game.feedback_bad') || ''}>
                            <IconButton onClick={() => handleFeedback('bad')} color="error">
                              <ThumbDownIcon />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </Box>
                    ) : (
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                        {t('game.feedback_thanks')}
                      </Typography>
                  )}

                  <Button
                    variant="contained"
                    color="secondary"
                    onClick={handleNextQuestionClick}
                    sx={{ mt: 2 }}
                    disabled={gameOver || isLoading}
                  >
                    {gameOver ? t('game.view_results') : t('game.next_question')}
                  </Button>
                </Box>
              )}
            </GameCard>
            {renderHintSection()}
          </Box>

          <Box sx={{ 
            flex: { xs: '1 1 auto', md: '0 0 55%' }, 
            display: 'flex', 
            flexDirection: 'column', 
            gap: { xs: 1.5, md: 2 },
            height: { xs: 'auto', md: '100%' }, 
            overflowY: 'auto' 
          }}>
            <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 1.5}}>
              <Box sx={{ flex: 1 }}>
                {person1InfoToDisplay && <PersonInfoDisplay personName={person1} personInfo={person1InfoToDisplay} />}
              </Box>
              <Box sx={{ flex: 1 }}>
                {person2InfoToDisplay && <PersonInfoDisplay personName={person2} personInfo={person2InfoToDisplay} isTarget />}
              </Box>
            </Box>
            
            <GameCard sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: '400px' }}>
              <Typography variant="subtitle1" fontWeight="medium" sx={{ p: 1, borderBottom: `1px solid ${theme.palette.divider}` }}>
                {t('relationship_map')} ({formatDifficulty(questionDifficulty)})
              </Typography>
              <Box sx={{ 
                flexGrow: 1, 
                position: 'relative', 
                width: '100%', 
                height: '350px',
                minHeight: '350px',
                maxHeight: '500px',
                '& > *': { width: '100%', height: '100%' }
              }}>
                <RelationshipGraph 
                  path={relationshipPathForGraph}
                  height="100%"
                  width="100%"
                  layoutDirection={optimalLayoutDirection}
                />
              </Box>
            </GameCard>
          </Box>
        </Box>
      </GamePlayArea>

      {showScores && (
        <Box sx={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, bgcolor: 'rgba(0,0,0,0.7)', display:'flex', alignItems:'center', justifyContent:'center', zIndex: 1201, p:2 }}>
          <Paper sx={{p:3, maxWidth:600, width:'95%', maxHeight:'90vh', overflowY:'auto'}}>
            <Box sx={{display:'flex', justifyContent:'space-between', alignItems:'center', mb:2}}>
              <Typography variant="h5">{t('high_scores.title')}</Typography>
              <IconButton onClick={() => setShowScores(false)}><CancelIcon/></IconButton>
            </Box>
            {renderHighScores()}
            <Button onClick={() => setShowScores(false)} variant="outlined" sx={{mt:2, display:'block', ml:'auto'}}>
              {t('close')}
            </Button>
          </Paper>
        </Box>
      )}
    </StyledContainer>
  );
};

export default FamilyRelationGame;