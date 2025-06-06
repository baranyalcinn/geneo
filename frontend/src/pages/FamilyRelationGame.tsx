import React, { useState, useEffect, useCallback, useRef } from 'react';
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
import { ReactFlowProvider } from 'reactflow';
import { useTranslation } from 'react-i18next';

import { 
  Difficulty,
  GameQuestion as GameQuestionType,
  GameAnswer,
  GameResult,
  PersonInfo,
  HighScores as HighScoresType,
  AnswerResponse,
  RelationshipStep
} from '../types/game';
import { 
  getQuestion, 
  submitAnswer, 
  getHighScores,
  recordGameResult
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
  borderRadius: theme.shape.borderRadius * 2,
  backgroundColor: alpha(theme.palette.background.paper, 0.7),
  overflow: 'auto',
  padding: theme.spacing(2),
  backdropFilter: 'blur(10px)',
  boxShadow: `0 4px 20px ${alpha(theme.palette.common.black, 0.15)}`,
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
  borderRadius: theme.shape.borderRadius * 1.5,
  boxShadow: `0 4px 12px ${alpha(theme.palette.primary.main, 0.15)}`,
  border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
}));

const GameCard = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  backgroundColor: theme.palette.background.paper,
  borderRadius: theme.shape.borderRadius * 1.5,
  boxShadow: `0 2px 10px ${alpha(theme.palette.common.black, 0.1)}`,
}));

const OptionButton = styled(FormControlLabel)(({ theme }) => ({
  width: '100%',
  margin: theme.spacing(0.5, 0),
  padding: theme.spacing(1),
  borderRadius: theme.shape.borderRadius,
  border: `1px solid ${theme.palette.divider}`,
  transition: 'all 0.2s ease',
  '&:hover': {
    backgroundColor: alpha(theme.palette.primary.main, 0.05),
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

// Tip tanımlaması düzeltmeleri
type HighScores = {
  playerName: string;
  score: number;
  difficulty?: string;
}[];

// GameAnswer tipini uyumlu hale getir
type ExtendedGameAnswer = Partial<GameAnswer> & {
  playerName: string;
  difficulty: Difficulty;
  askedQuestionSignaturesInThisGame: string[];
  currentScore: number;
  currentStreak: number;
  timeTakenInSeconds: number;
  questionId: string;
  answer: string;
};

const createSimpleRelationshipPath = (person1Info: PersonInfo, person2Info: PersonInfo): RelationshipStep[] => {
  if (!person1Info || !person2Info) return [];
  
  const person1Id = typeof person1Info.id === 'string' ? parseInt(person1Info.id, 10) : (person1Info.id || 0);
  const person2Id = typeof person2Info.id === 'string' ? parseInt(person2Info.id, 10) : (person2Info.id || 0);
  
  const person1BirthYear = person1Info.birthYear ? parseInt(String(person1Info.birthYear), 10) : undefined;
  const person2BirthYear = person2Info.birthYear ? parseInt(String(person2Info.birthYear), 10) : undefined;
  
  return [
    {
      personId: person1Id,
      personName: person1Info.fullName || "",
      personGender: person1Info.gender || "Bilinmiyor",
      personBirthYear: person1BirthYear,
      relationshipToNextPerson: "İlişki",
      sourcePerson: true,
      targetPerson: false
    },
    {
      personId: person2Id,
      personName: person2Info.fullName || "",
      personGender: person2Info.gender || "Bilinmiyor",
      personBirthYear: person2BirthYear,
      sourcePerson: false,
      targetPerson: true
    }
  ];
};

const FamilyRelationGame = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { t, i18n } = useTranslation();
  
  const [gameStarted, setGameStarted] = useState(false);
  const [score, setScore] = useState(0);
  const [showResult, setShowResult] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState('');
  const [isCorrect, setIsCorrect] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState<GameQuestionType | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [correctAnswers, setCorrectAnswers] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(0);
  const [currentStreak, setCurrentStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [showHint, setShowHint] = useState(false);
  const [gameMode, setGameMode] = useState<'classic' | 'timed' | 'path'>('classic');
  const [showPath, setShowPath] = useState(false);
  const [answerResult, setAnswerResult] = useState<GameResult | null>({
    playerName: '',
    score: 0,
    difficulty: Difficulty.EASY,
    correctAnswers: 0,
    totalQuestions: 0,
    maxStreak: 0,
    askedQuestionSignaturesInThisGame: [],
    correct: false,
    gameOver: false
  } as any);
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  
  // Timer 
  const [timeLeft, setTimeLeft] = useState(0);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const [difficulty, setDifficulty] = useState<Difficulty>(Difficulty.MEDIUM);
  const [playerName, setPlayerName] = useState('');
  const [highScores, setHighScores] = useState<HighScoresType | null>(null);
  const [showScores, setShowScores] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');

  const gameAreaRef = useRef<HTMLDivElement>(null);

  // Kişi bilgilerini elde etme fonksiyonu
  const getPersonInfoFromName = (personNameKey: string, path?: RelationshipStep[]): PersonInfo => {
    if (!path) return { id: 0, fullName: personNameKey };
    
    const foundStep = path.find(step => step.personName === personNameKey);
    if (!foundStep) return { id: 0, fullName: personNameKey };
    
    return {
      id: foundStep.personId,
      fullName: foundStep.personName,
      gender: foundStep.personGender,
      birthYear: foundStep.personBirthYear
    };
  };
  
  // Debug bilgilerini göstermek için useEffect - bileşenin en üst seviyesinde
  useEffect(() => {
    if (currentQuestion) {
      let path = currentQuestion.relationshipPath;
      let graphAvailable = Array.isArray(path) && path.length > 0;
      const person1 = currentQuestion.person1;
      const person2 = currentQuestion.person2;
      
      // Eğer path yoksa veya boşsa ve kişi bilgileri varsa basit bir yol oluştur
      if (!graphAvailable && currentQuestion.person1Info && currentQuestion.person2Info) {
        path = createSimpleRelationshipPath(currentQuestion.person1Info, currentQuestion.person2Info);
        graphAvailable = path.length > 0;
        
        // PathLength ve graphAvailable değerlerini güncelle
        console.log("Basit ilişki yolu oluşturuldu: ", path);
      }
      
      // Bu fonksiyon sadece useEffect içinde kullanılıyor, bağımlılık listesine eklemeye gerek yok
      const getInfoFromPath = (personNameKey: string, path?: RelationshipStep[]): PersonInfo => {
        if (!path) return { id: 0, fullName: personNameKey };
        
        const foundStep = path.find(step => step.personName === personNameKey);
        if (!foundStep) return { id: 0, fullName: personNameKey };
        
        return {
          id: foundStep.personId,
          fullName: foundStep.personName,
          gender: foundStep.personGender,
          birthYear: foundStep.personBirthYear
        };
      };
      
      const person1Info = currentQuestion.person1Info || getInfoFromPath(person1, path);
      const person2Info = currentQuestion.person2Info || getInfoFromPath(person2, path);
      
      console.log("İlişki şeması verisi:", { 
        graphAvailable, 
        pathLength: path?.length || 0, 
        path,
        person1: person1Info,
        person2: person2Info
      });
      
      if (graphAvailable) {
        console.log("İlişki haritası gösterilecek:", JSON.stringify(path));
      } else {
        console.log("İlişki haritası gösterilmeyecek: Veri yok veya boş dizi");
      }
    }
  }, [currentQuestion]);

  // Yüksek skorları getir
  const fetchHighScores = useCallback(async () => {
    try {
      const scoresFromApi = await getHighScores(); 
      setHighScores(scoresFromApi); 
    } catch (err) {
      console.error('Yüksek skorlar getirilirken hata oluştu:', err);
      setHighScores(null);
    }
  }, []);

  useEffect(() => {
    // Backend API'sini kullanarak yüksek skorları getir
    fetchHighScores();
    
    // API'den alamazsak, varsayılan değerler kullanılacak (catch bloğunda)
  }, [fetchHighScores]);

  // Timer'ı başlat
  useEffect(() => {
    if (gameStarted && currentQuestion && timeLeft > 0) {
      timerRef.current = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            handleTimerEnd();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [gameStarted, currentQuestion, timeLeft]);

  // Süre bitince
  const handleTimerEnd = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    if (currentQuestion && !showResult) {
      checkAnswer('');
    }
  };

  const handleSubmitAnswer = async (answerText: string) => {
    if (!currentQuestion || showResult) return;
    
    setIsLoading(true);
    setError(null);
    setSelectedAnswer(answerText);

    try {
      // Sorulan soruların id'lerini topla
      const askedQuestionSignaturesInThisGame = [
        ...(answerResult?.askedQuestionSignaturesInThisGame || []),
        currentQuestion.id
      ];
      const gameAnswerPayload = {
        questionId: currentQuestion.id,
        answer: answerText,
        playerName: playerName,
        difficulty: currentQuestion.difficulty,
        askedQuestionSignaturesInThisGame,
        currentScore: score,
        currentStreak: currentStreak,
        timeTakenInSeconds: (currentQuestion.timeLimit || 60) - timeLeft,
        sessionId: sessionId
      } as any;
      const apiResult = await submitAnswer(gameAnswerPayload, i18n.language) as any;
      const gameResult: GameResult = {
        correct: apiResult.correct || false,
        score: apiResult.score || 0,
        gameOver: apiResult.gameOver || false,
        message: apiResult.message || "",
        playerName: apiResult.playerName || playerName,
        difficulty: apiResult.difficulty || difficulty,
        correctAnswers: apiResult.correctAnswers || 0,
        totalQuestions: apiResult.totalQuestions || 0,
        maxStreak: apiResult.maxStreak || 0
      };
      
      setScore(gameResult.score);
      setIsCorrect(!!gameResult.correct);
      setShowResult(true);

      if (gameResult.correct) {
        setCorrectAnswers(prev => prev + 1);
        setCurrentStreak(prev => prev + 1);
        setScore(prev => prev + gameResult.score);
        setMaxStreak(prev => Math.max(prev, currentStreak + 1));
      } else {
        setCurrentStreak(0);
      }
      
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }

      setTimeout(() => {
        fetchNextQuestion();
      }, 2000);

    } catch (err: any) {
      setError(err.message || 'Cevap gönderilirken bir hata oluştu.');
      setIsLoading(false);
      setTimeout(() => {
        fetchNextQuestion();
      }, 3000);
    }
  };

  // Yeni soru yükle
  const fetchNextQuestion = async () => {
    if (gameOver) return;
    setIsLoading(true);
    setError(null);
    try {
      // In a real game, the next question comes from the submitAnswer response.
      // This is a fallback or for a different mode.
      const questionData = await getQuestion(difficulty, i18n.language);
      setCurrentQuestionSafely(questionData);
    } catch (err) {
      setError(t('error.question_load'));
      console.error(err);
    } finally {
      setIsLoading(false);
      setShowResult(false);
      setSelectedAnswer('');
    }
  };

  const startGame = async () => {
    if (!playerName.trim()) {
      setError(t('player_name.required')); // Example of a new key
      return;
    }
    setIsLoading(true);
    setError(null);
    setGameOver(false);
    setGameStarted(true);
    setScore(0);
    setCorrectAnswers(0);
    setTotalQuestions(0);
    setCurrentStreak(0);
    setMaxStreak(0);
    
    // Yeni oturum ID'si oluştur
    const newSessionId = `game_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    setSessionId(newSessionId);
    
    try {
      console.log(`Oyun başlatılıyor: ${playerName}, Zorluk: ${difficulty}, Oturum ID: ${newSessionId}`);
      // Oyuncu adını yerel depolamaya kaydet
      localStorage.setItem('lastPlayerName', playerName);
      
      // Başlangıç sorusunu getir
      try {
        const questionData = await getQuestion(difficulty, i18n.language);
        console.log("İlk soru alındı:", questionData);
        
        if (!questionData || !questionData.id) {
          setError("Soru yüklenemedi. Lütfen tekrar deneyin.");
          setGameStarted(false);
          setIsLoading(false);
          return;
        }
        
        // İlk soruyu ayarla (null olma olasılığına karşı güvenli bir şekilde)
        if (questionData) {
          setCurrentQuestionSafely(questionData);
          setTimeLeft(questionData.timeLimit || 60);
        } else {
          throw new Error("Soru alınamadı");
        }
      } catch (questionErr: any) {
        console.error("Soru alınırken hata:", questionErr);
        const errorMessage = questionErr?.message || "Bilinmeyen soru hatası";
        setError(`Soru alınırken hata: ${errorMessage}`);
        setGameStarted(false);
        setIsLoading(false);
        return;
      }
      
      setIsLoading(false);
      
      // Zamanlayıcıyı başlat
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
      
      timerRef.current = setInterval(() => {
        setTimeLeft(prevTime => {
          if (prevTime <= 1) {
            if (timerRef.current) {
              clearInterval(timerRef.current);
            }
            checkAnswer('');
            return 0;
          }
          return prevTime - 1;
        });
      }, 1000);
      
    } catch (err: any) {
      console.error("Oyun başlatılırken hata:", err);
      setError(err.message || "Oyun başlatılırken bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
      setGameStarted(false);
      setIsLoading(false);
    }
  };

  const handleDifficultyChange = (event: SelectChangeEvent<string>) => {
    setDifficulty(event.target.value as Difficulty);
  };

  const checkAnswer = async (answer: string) => {
    if (!currentQuestion || !sessionId) return;

    setIsLoading(true);
    setError(null);

    const answerPayload: GameAnswer = {
      questionId: currentQuestion.id,
      answer: answer,
      sessionId: sessionId,
      difficulty: difficulty,
      playerName: playerName,
      timeTakenInSeconds: timeLimit - timeLeft,
      askedQuestionSignaturesInThisGame: [], // This should be managed properly
      currentScore: score,
      currentStreak: currentStreak,
    };

    try {
      const result = await submitAnswer(answerPayload, i18n.language);
      
      setScore(result.updatedScore);
      setCurrentStreak(result.updatedStreak);
      
      // Cevabın doğruluğunu kontrol et
      const isAnswerCorrect = result.correctAnswer;
      setIsCorrect(isAnswerCorrect);
      
      // Puanla ilgili değerleri güncelle
      setScore(result.updatedScore);
      
      // Soru kontrolünü tamamla ve sonuçları göster
      setShowResult(true);
      setIsLoading(false);
      
      // Oyun bitmiş mi kontrol et
      if (result.gameOver) {
        handleGameOver(result.finalResult);
      } else {
        // Sonraki soruya hazırlan
        setTimeout(() => {
          fetchNextQuestion();
        }, 2000); // 2 saniye beklet
      }
      
    } catch (err: any) {
      console.error("Cevap kontrol edilirken hata:", err);
      setError(err.message || "Cevabınız kontrol edilirken bir hata oluştu.");
      setIsLoading(false);
      setTimeout(() => {
        fetchNextQuestion();
      }, 2000);
    }
  };

  const restartGame = () => {
    setGameStarted(false);
    setScore(0);
    setGameOver(false);
    setShowResult(false);
    setSelectedAnswer('');
    setShowScores(false);
    setCorrectAnswers(0);
    setTotalQuestions(0);
    setCurrentStreak(0);
    setMaxStreak(0);
    setCurrentQuestion(null);
    setError(null);
    setShowHint(false);
    setShowPath(false);
    
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    setTimeLeft(0);
  };

  const formatDifficulty = (diff: string): string => {
    switch (diff) {
      case Difficulty.EASY: return 'Kolay';
      case Difficulty.MEDIUM: return 'Orta';
      case Difficulty.HARD: return 'Zor';
      default: return diff;
    }
  };

  const toggleShowPath = () => {
    setShowPath(prev => !prev);
  };

  const toggleHint = () => {
    setShowHint(!showHint);
    if (gameAreaRef.current) {
      gameAreaRef.current.focus();
    }
  };

  const getHintByDifficulty = (): { generalHint: string, specificHints: string[] } => {
    if (!currentQuestion) return { generalHint: "", specificHints: [] };

    const person1 = getPersonInfoFromName(currentQuestion.person1, currentQuestion.relationshipPath);
    const person2 = getPersonInfoFromName(currentQuestion.person2, currentQuestion.relationshipPath);

    const hints = {
      generalHint: "",
      specificHints: [] as string[]
    };

    switch (currentQuestion.difficulty) {
      case Difficulty.EASY:
        hints.generalHint = "Birinci derece akrabalık ilişkisi (doğrudan bağlantı)";
        hints.specificHints = [
          "Anne/baba, kardeş veya eş ilişkisi olabilir",
          "İsimler ve yaşları karşılaştırabilirsiniz",
          "Cinsiyet bilgisi önemli bir ipucu olabilir"
        ];
        break;
      case Difficulty.MEDIUM:
        hints.generalHint = "İkinci derece akrabalık ilişkisi";
        hints.specificHints = [
          "Amca/dayı/hala/teyze veya yeğen ilişkisi olabilir",
          "Büyükanne/büyükbaba veya torun ilişkisi olabilir",
          "Doğum tarihlerini ve cinsiyetleri dikkate alın"
        ];
        break;
      case Difficulty.HARD:
        hints.generalHint = "Karmaşık veya uzak akrabalık ilişkisi";
        hints.specificHints = [
          "Kayın ilişkileri (kayınvalide, kayınpeder, baldız vb.)",
          "Kuzenler veya uzak akrabalar",
          "İlişki birden fazla adım içerebilir"
        ];
        break;
      default:
        hints.generalHint = "İlişkiyi anlamak için detaylara dikkat edin";
    }

    return hints;
  };

  const renderHintSection = () => {
    if (!currentQuestion || !showHint) return null;

    const hintData = getHintByDifficulty();
    
    let hintContent = null;
    
    if (hintData) {
      hintContent = (
        <GameCard sx={{ mt: 2, background: alpha(theme.palette.info.light, 0.05) }}>
          <Typography variant="h6" gutterBottom sx={{ color: theme.palette.info.main, display: 'flex', alignItems: 'center' }}>
            <HelpOutlineIcon sx={{ mr: 1 }} /> {t('hint.title')}
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>{hintData.generalHint}</Typography>
          {hintData.specificHints.length > 0 && (
            <Box>
              {hintData.specificHints.map((hint, index) => (
                <Chip key={index} label={hint} size="small" sx={{ mr: 0.5, mb: 0.5, background: alpha(theme.palette.info.main, 0.1) }} />
              ))}
            </Box>
          )}
        </GameCard>
      );
    }
    
    return hintContent;
  };

  const renderGameSetup = () => {
    return (
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
            background: `linear-gradient(135deg, ${alpha(theme.palette.primary.light, 0.05)} 0%, ${alpha(theme.palette.background.paper, 0.75)} 100%)`,
            borderRadius: theme.shape.borderRadius * 1.5,
            backdropFilter: 'blur(10px)',
          }}
        >
          <Paper 
            elevation={3}
            sx={{
              p: { xs: 2, sm: 3 },
              borderRadius: theme.shape.borderRadius * 2,
              width: '100%',
              maxWidth: 500,
              textAlign: 'center',
              background: alpha(theme.palette.background.paper, 0.9),
              backdropFilter: 'blur(15px)',
              border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
              boxShadow: `0 10px 40px ${alpha(theme.palette.primary.dark, 0.15)}`,
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
                  boxShadow: `0 8px 25px ${alpha(theme.palette.primary.main, 0.25)}`,
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

            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3 }}>
              <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '200px' }}>
                <FormControl fullWidth variant="outlined" size="small">
                  <InputLabel>{t('difficulty.label')}</InputLabel>
                  <Select
                    value={difficulty}
                    onChange={handleDifficultyChange}
                    label={t('difficulty.label')}
                  >
                    <MenuItem value={Difficulty.EASY}>{t('difficulty.easy')}</MenuItem>
                    <MenuItem value={Difficulty.MEDIUM}>{t('difficulty.medium')}</MenuItem>
                    <MenuItem value={Difficulty.HARD}>{t('difficulty.hard')}</MenuItem>
                  </Select>
                </FormControl>
              </Box>
              <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '200px' }}>
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
              </Box>
              <Box sx={{ width: '100%' }}>
                <FormControl component="fieldset" sx={{ width: '100%' }}>
                  <Typography variant="subtitle2" sx={{ mb: 1, textAlign: 'left', fontWeight: 'medium' }}>
                    {t('game_mode.label')}:
                  </Typography>
                  <RadioGroup
                    row
                    value={gameMode}
                    onChange={(e) => setGameMode(e.target.value as 'classic' | 'timed' | 'path')}
                    sx={{ justifyContent: 'space-between' }}
                  >
                    <Paper 
                      elevation={gameMode === 'classic' ? 2 : 0}
                      sx={{
                        borderRadius: 2,
                        overflow: 'hidden',
                        flex: 1,
                        maxWidth: 145,
                        border: `1px solid ${gameMode === 'classic' ? theme.palette.primary.main : theme.palette.divider}`,
                        transition: 'all 0.2s ease',
                        transform: gameMode === 'classic' ? 'scale(1.05)' : 'scale(1)',
                      }}
                    >
                      <FormControlLabel 
                        value="classic" 
                        control={<Radio sx={{ '.MuiSvgIcon-root': { fontSize: 18 } }} />} 
                        label={t('game_mode.classic')} 
                        sx={{ 
                          m: 0, 
                          p: 1, 
                          width: '100%',
                          height: '100%',
                          background: gameMode === 'classic' ? alpha(theme.palette.primary.light, 0.1) : 'transparent',
                        }}
                      />
                    </Paper>
                    <Paper 
                      elevation={gameMode === 'timed' ? 2 : 0}
                      sx={{
                        borderRadius: 2,
                        overflow: 'hidden',
                        flex: 1,
                        maxWidth: 145,
                        border: `1px solid ${gameMode === 'timed' ? theme.palette.primary.main : theme.palette.divider}`,
                        transition: 'all 0.2s ease',
                        transform: gameMode === 'timed' ? 'scale(1.05)' : 'scale(1)',
                      }}
                    >
                      <FormControlLabel 
                        value="timed" 
                        control={<Radio sx={{ '.MuiSvgIcon-root': { fontSize: 18 } }} />} 
                        label={t('game_mode.timed')} 
                        sx={{ 
                          m: 0, 
                          p: 1, 
                          width: '100%',
                          background: gameMode === 'timed' ? alpha(theme.palette.primary.light, 0.1) : 'transparent',
                        }}
                      />
                    </Paper>
                    <Paper 
                      elevation={gameMode === 'path' ? 2 : 0}
                      sx={{
                        borderRadius: 2,
                        overflow: 'hidden',
                        flex: 1,
                        maxWidth: 145,
                        border: `1px solid ${gameMode === 'path' ? theme.palette.primary.main : theme.palette.divider}`,
                        transition: 'all 0.2s ease',
                        transform: gameMode === 'path' ? 'scale(1.05)' : 'scale(1)',
                      }}
                    >
                      <FormControlLabel 
                        value="path" 
                        control={<Radio sx={{ '.MuiSvgIcon-root': { fontSize: 18 } }} />} 
                        label={t('game_mode.path')} 
                        sx={{ 
                          m: 0, 
                          p: 1, 
                          width: '100%',
                          background: gameMode === 'path' ? alpha(theme.palette.primary.light, 0.1) : 'transparent',
                        }}
                      />
                    </Paper>
                  </RadioGroup>
                </FormControl>
              </Box>
            </Box>

            <Button
              variant="contained"
              color="primary"
              size="large"
              startIcon={<SportsEsportsIcon />}
              onClick={startGame}
              disabled={isLoading}
              sx={{ 
                mt: 1, 
                py: 1.5, 
                minWidth: '50%',
                borderRadius: 3,
                boxShadow: `0 4px 15px ${alpha(theme.palette.primary.main, 0.4)}`,
                background: `linear-gradient(45deg, ${theme.palette.primary.dark} 0%, ${theme.palette.primary.main} 100%)`,
                '&:hover': {
                  background: `linear-gradient(45deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                  boxShadow: `0 6px 20px ${alpha(theme.palette.primary.main, 0.6)}`,
                  transform: 'translateY(-2px)',
                },
              }}
            >
              {isLoading ? <CircularProgress size={24} color="inherit" /> : t('start_game')}
            </Button>
            
            <Button
              variant="outlined"
              size="small"
              color="primary"
              startIcon={<LeaderboardIcon />}
              onClick={() => setShowScores(true)}
              sx={{ mt: 2, borderRadius: 2 }}
            >
              {t('high_scores')}
            </Button>
          </Paper>
        </Box>
      </GamePlayArea>
    );
  };

  const renderGameOver = () => {
    const accuracy = totalQuestions > 0 ? (correctAnswers / totalQuestions) * 100 : 0;
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
        <Typography variant="h6" sx={{ mb: 1 }}>{playerName}, {t('game_over.score')}: {score}</Typography>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', mb: 2 }}>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.total_questions')}: {totalQuestions}</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.correct_answers')}: {correctAnswers}</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.accuracy')}: {accuracy.toFixed(1)}%</Typography>
          </Box>
          <Box sx={{ flex: '1 1 calc(50% - 16px)', minWidth: '120px', p: 1 }}>
            <Typography variant="body1">{t('game_over.max_streak')}: {maxStreak}</Typography>
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
            {t('high_scores')}
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

  // Oyun bitince skor kaydetme fonksiyonu
  const handleGameOver = async (finalResultParam?: GameResult | null) => {
    setGameOver(true);
    
    const resultToSave = finalResultParam || answerResult;

    if (resultToSave) {
      setAnswerResult(resultToSave);
      try {
        const savedResult = await recordGameResult(resultToSave);
        console.log("Oyun sonucu kaydedildi:", savedResult);
        fetchHighScores();
      } catch (err) {
        console.error("Skor kaydedilirken hata:", err);
      }
    }
  };

  // Ana bileşenin en üst seviyesinde useEffect ekleyelim
  useEffect(() => {
    // Sadece oyun başlamamışsa son oynanmış oyuncu adını getir
    if (!gameStarted) {
      const lastPlayerName = localStorage.getItem('lastPlayerName');
      if (lastPlayerName) {
        setPlayerName(lastPlayerName);
      }
    }
  }, [gameStarted]);

  const setCurrentQuestionSafely = (questionData: GameQuestionType | null) => {
    if (questionData) {
      // Gelen verinin temel alanlara sahip olduğundan emin olalım, ancak "Kişi 1" gibi varsayılanlar atamayalım.
      const safeQuestion: GameQuestionType = {
        ...questionData,
        id: questionData.id || `question-${Date.now()}`,
        options: questionData.options || [],
        person1Info: questionData.person1Info || { id: 'unknown1', fullName: questionData.person1 || '' },
        person2Info: questionData.person2Info || { id: 'unknown2', fullName: questionData.person2 || '' },
        relationshipPath: questionData.relationshipPath || createSimpleRelationshipPath(questionData.person1Info!, questionData.person2Info!)
      };
      setCurrentQuestion(safeQuestion);
    } else {
      setCurrentQuestion(null);
    }
  };

  // HighScores render fonksiyonu
  const renderHighScores = () => {
    if (!highScores) return null;
    
    // highScores'un tip kontrolünü yap ve tip tanımlamalarını ekle
    type ScoreItem = {
      playerName: string;
      score: number;
      difficulty?: string;
    };
    
    let renderedScores: ScoreItem[] = [];
    
    if (Array.isArray(highScores)) {
      renderedScores = [...highScores]
        .sort((a: ScoreItem, b: ScoreItem) => b.score - a.score)
        .slice(0, 10);
    }
    
    return (
      <TableContainer component={Paper} elevation={0} variant="outlined">
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell>{t('high_scores.rank')}</TableCell>
              <TableCell>{t('high_scores.player')}</TableCell>
              <TableCell>{t('high_scores.score')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {renderedScores.map((s: ScoreItem, i: number) => (
              <TableRow key={i} hover>
                <TableCell>{i+1}</TableCell>
                <TableCell>{s.playerName}</TableCell>
                <TableCell sx={{fontWeight:'bold'}}>{s.score}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    );
  };

  const handleLanguageChange = (lang: string) => {
    i18n.changeLanguage(lang);
  };

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
    return <ErrorMessage message={`${t('error.game_load')}: ${error}`} />;
  }
  
  if (!currentQuestion) {
    return <ErrorMessage message={t('error.question_load')} />;
  }

  const { person1, person2, options, timeLimit, difficulty: currentDifficulty, relationshipPath: currentRelationshipPath } = currentQuestion;
  const person1InfoToDisplay: PersonInfo = currentQuestion.person1Info || getPersonInfoFromName(person1, currentRelationshipPath);
  const person2InfoToDisplay: PersonInfo = currentQuestion.person2Info || getPersonInfoFromName(person2, currentRelationshipPath);

  const graphAvailable = Array.isArray(currentRelationshipPath) && currentRelationshipPath.length > 0;
  
  const timeProgress = (timeLimit || 0) > 0 ? (timeLeft / (timeLimit || 60)) * 100 : 0;

  return (
    <StyledContainer maxWidth="xl">
      <HeaderBar>
        <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center' }}>
          <SportsEsportsIcon sx={{ mr: 1 }} /> {t('game.title')}
        </Typography>
        <Box>
          <Button onClick={() => handleLanguageChange('en')} disabled={i18n.language === 'en'}>EN</Button>
          <Button onClick={() => handleLanguageChange('tr')} disabled={i18n.language === 'tr'}>TR</Button>
          <Button startIcon={<RefreshIcon />} onClick={restartGame} sx={{ mr: 1 }}>{t('restart_game')}</Button>
          <Button startIcon={<HomeIcon />} onClick={() => navigate('/')}>{t('home')}</Button>
        </Box>
      </HeaderBar>
      <GamePlayArea ref={gameAreaRef} tabIndex={-1}>
        {isLoading && <LoadingIndicator />}
        {error && <ErrorMessage message={error} />}

        <Box sx={{ 
          display: 'flex', 
          flexDirection: { xs: 'column', md: 'row' }, 
          flexGrow: 1, 
          gap: { xs: 1.5, md: 2 },
          height: '100%'
        }}>
          {/* Sol Panel - Soru ve Cevap Kısmı */}
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
                icon={<EmojiEventsIcon sx={{fontSize: '1rem'}} />} 
                label={`${t('game.score')}: ${score}`} 
                color="primary" 
                variant="filled" 
                size="small" 
                sx={{ 
                  fontSize: '0.8rem', 
                  py: 0.5, 
                  fontWeight: 'bold',
                  height: 28
                }} 
              />
              <Chip 
                icon={<TimerIcon sx={{fontSize: '1rem'}} />} 
                label={`${t('streak')}: ${currentStreak}`} 
                color="secondary" 
                variant="filled" 
                size="small" 
                sx={{ 
                  fontSize: '0.8rem', 
                  py: 0.5, 
                  fontWeight: 'bold',
                  height: 28
                }} 
              />
              <Chip 
                label={`${t('streak')}: ${currentStreak}/${maxStreak}`} 
                size="small" 
                variant="outlined" 
                sx={{ 
                  fontSize: '0.75rem',
                  fontWeight: '500',
                  height: 28
                }} 
              />
            </ScoreTimeDisplay>

            {timeLimit !== undefined && (timeLimit || 0) > 0 && (
              <Box sx={{ width: '100%', mb: 1 }}>
                <Box sx={{ 
                  height: 10, 
                  backgroundColor: alpha(theme.palette.secondary.light, 0.3), 
                  borderRadius: theme.shape.borderRadius * 2,
                  overflow: 'hidden',
                  boxShadow: `inset 0 1px 3px ${alpha(theme.palette.common.black, 0.15)}`
                }}>
                  <Box sx={{ 
                    width: `${timeProgress}%`, 
                    height: '100%', 
                    backgroundColor: timeLeft < 10 
                      ? theme.palette.error.main
                      : timeLeft < 20 
                        ? theme.palette.warning.main 
                        : theme.palette.secondary.main, 
                    borderRadius: theme.shape.borderRadius * 2, 
                    transition: 'width 0.5s ease-in-out, background-color 0.5s ease' 
                  }} />
                </Box>
              </Box>
            )}
            
            <QuestionCard elevation={3}>
              <Typography 
                variant="h6" 
                component="h2" 
                sx={{ 
                  mb: 1, 
                  fontWeight: '600', 
                  color: theme.palette.primary.dark, 
                  fontSize: { xs: '0.95rem', sm: '1.1rem' }
                }}
              >
                {t('game.question_title', { number: totalQuestions + 1 })}
              </Typography>
              <Typography 
                variant="subtitle1" 
                sx={{ 
                  mb: 1, 
                  color: theme.palette.text.primary, 
                  fontWeight: "500", 
                  fontSize: { xs: '0.9rem', sm: '1rem' },
                  lineHeight: 1.4
                }}
              >
                {t('game.question', { person1: person1, person2: person2 })}
              </Typography>
            </QuestionCard>

            <GameCard elevation={2}>
              <Typography 
                variant="subtitle2" 
                fontWeight="600" 
                sx={{ 
                  mb: 1, 
                  color: theme.palette.text.primary, 
                  fontSize: '0.9rem',
                  borderBottom: `1px solid ${alpha(theme.palette.divider, 0.5)}`,
                  pb: 0.5
                }}
              >
                {t('answer_options')}:
              </Typography>
              <RadioGroup
                aria-label="answer"
                name="answer-radio-buttons-group"
                value={selectedAnswer}
                onChange={(e) => setSelectedAnswer(e.target.value)}
              >
                {options.map((option) => (
                  <OptionButton
                    key={option}
                    value={option}
                    control={<Radio sx={{visibility: 'hidden', width:0, height:0, p:0, m:0}} />}
                    label={
                      <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                        <Typography 
                          variant="body1" 
                          sx={{ 
                            flexGrow: 1, 
                            fontSize: '0.95rem',
                            fontWeight: '500'
                          }}
                        >
                          {t(option as any)}
                        </Typography>
                        {showResult && selectedAnswer === option && (
                          isCorrect ? <CheckCircleIcon color="success" fontSize="small" /> : <CancelIcon color="error" fontSize="small" />
                        )}
                        {showResult && currentQuestion.correctAnswer === option && selectedAnswer !== option && (
                          <CheckCircleIcon color="disabled" fontSize="small" />
                        )}
                      </Box>
                    }
                    disabled={showResult || isLoading}
                    sx={{
                      ...(showResult && currentQuestion.correctAnswer === option && {
                        borderColor: theme.palette.success.main,
                        backgroundColor: alpha(theme.palette.success.light, 0.15),
                      }),
                      ...(showResult && selectedAnswer === option && !isCorrect && {
                        borderColor: theme.palette.error.main,
                        backgroundColor: alpha(theme.palette.error.light, 0.15),
                      }),
                      '&.Mui-disabled': {
                          opacity: 0.8,
                      }
                    }}
                    onClick={() => !showResult && !isLoading && checkAnswer(option)}
                  />
                ))}
              </RadioGroup>
              {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
              <Box sx={{ 
                mt: 3, 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center',
                pt: 1.5,
                borderTop: `1px solid ${alpha(theme.palette.divider, 0.5)}`
              }}>
                <AnimatedButton
                  variant="contained"
                  color="primary"
                  onClick={() => checkAnswer(selectedAnswer)}
                  disabled={!selectedAnswer || showResult || isLoading}
                  startIcon={isLoading ? <CircularProgress size={20} color="inherit" /> : <CheckCircleIcon />}
                  sx={{
                    fontSize: '0.95rem',
                    textTransform: 'none'
                  }}
                >
                  {isLoading ? t('checking_answer') : (showResult ? (isCorrect ? t('correct') : t('incorrect')) : t('game.check_answer'))}
                </AnimatedButton>
                <Tooltip title={showHint ? t('hide_hint') : t('show_hint')}>
                  <IconButton 
                    onClick={toggleHint} 
                    color="info" 
                    sx={{
                      boxShadow: showHint ? `0 0 0 2px ${alpha(theme.palette.info.main, 0.35)}` : 'none',
                      backgroundColor: showHint ? alpha(theme.palette.info.main, 0.1) : 'transparent',
                      '&:hover': {
                        backgroundColor: alpha(theme.palette.info.main, 0.15),
                      }
                    }}
                  >
                    <HelpOutlineIcon />
                  </IconButton>
                </Tooltip>
              </Box>
            </GameCard>
            {renderHintSection()}
          </Box>

          {/* Sağ Panel - Kişi Bilgileri ve Harita */}
          <Box sx={{ 
            flex: { xs: '1 1 auto', md: '0 0 55%' }, 
            display: 'flex', 
            flexDirection: 'column', 
            gap: { xs: 1.5, md: 2 },
            height: { xs: 'auto', md: '100%' }, 
            overflowY: 'auto' 
          }}>
            <Box sx={{ 
              display: 'flex', 
              flexDirection: { xs: 'column', sm: 'row' }, 
              gap: 1.5,
              flex: '0 0 auto'
            }}>
              <Box sx={{ flex: '1 1 50%' }}>
                <PersonInfoDisplay personName={person1} personInfo={person1InfoToDisplay} />
              </Box>
              <Box sx={{ flex: '1 1 50%' }}>
                <PersonInfoDisplay personName={person2} personInfo={person2InfoToDisplay} isTarget />
              </Box>
            </Box>
            
            <GameCard sx={{ 
              flexGrow: 1, 
              display: 'flex', 
              flexDirection: 'column', 
              overflow: 'hidden', 
              background: alpha(theme.palette.background.paper, 0.97),
              border: `1px solid ${alpha(theme.palette.primary.main, 0.15)}` 
            }}>
              <Typography 
                variant="subtitle2" 
                fontWeight="medium" 
                sx={{ 
                  mb: 0.25,
                  color: theme.palette.text.primary, 
                  p: 0.5,
                  background: alpha(theme.palette.primary.light, 0.12), 
                  borderRadius: 1, 
                  fontSize: '0.85rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <span>{t('relationship_map')} ({formatDifficulty(currentDifficulty)})</span>
                <Tooltip title={t('relationship_map_tooltip')}>
                  <InfoIcon fontSize="small" sx={{ color: theme.palette.primary.main, opacity: 0.8 }} />
                </Tooltip>
              </Typography>
              <Box sx={{ 
                flexGrow: 1, 
                height: '350px',
                borderRadius: theme.shape.borderRadius * 1.5, 
                overflow: 'hidden', 
                position: 'relative', 
                minHeight: { xs: 200, sm: 220, md: 280 },
                border: `1px solid ${alpha(theme.palette.primary.main, 0.25)}`,
                background: theme.palette.mode === 'dark' 
                  ? alpha(theme.palette.grey[900], 0.8)
                  : alpha(theme.palette.grey[100], 0.8),
                boxShadow: `inset 0 2px 6px ${alpha(theme.palette.common.black, 0.08)}`,
                display: 'flex'
              }}>
                {isLoading && <CircularProgress sx={{position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 10}} />}
                
                {!isLoading && (
                  <Box sx={{ 
                    width: '100%', 
                    height: '350px',
                    position: 'relative',
                    display: 'flex',
                    flexGrow: 1
                  }}>
                    <ReactFlowProvider>
                      <RelationshipGraph 
                        path={Array.isArray(currentRelationshipPath) && currentRelationshipPath.length > 0 
                          ? currentRelationshipPath as any
                          : createSimpleRelationshipPath(person1InfoToDisplay, person2InfoToDisplay) as any}
                        height="100%"
                        width="100%"
                      />
                    </ReactFlowProvider>
                  </Box>
                )}
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