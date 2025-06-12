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
import { useGameSession } from '../hooks/useGameSession';
import { toast } from 'react-hot-toast';
import { useGameStore } from '../store/gameStore';

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
  askedQuestionSignaturesInThisGame: string[];
  currentScore: number;
  currentStreak: number;
  timeTakenInSeconds: number;
  questionId: string;
  answer: string;
};

const FamilyRelationGame = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { t, i18n } = useTranslation();
  const { 
    startGame: startGameSessionApi, 
    currentSession, 
    gameStarted: isGameSessionActive 
  } = useGameSession();
  
  const {
    currentQuestion,
    setCurrentQuestion,
    isLoading,
    setLoading,
    error,
    setError
  } = useGameStore();
  
  const [gameStarted, setGameStarted] = useState(false);
  const [score, setScore] = useState(0);
  const [showResult, setShowResult] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState('');
  const [isCorrect, setIsCorrect] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const [correctAnswers, setCorrectAnswers] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(0);
  const [currentStreak, setCurrentStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [timeRemaining, setTimeRemaining] = useState(30);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const [lastAnswerResponse, setLastAnswerResponse] = useState<AnswerResponse | null>(null);
  const [askedQuestions, setAskedQuestions] = useState<string[]>([]);
  const [highScores, setHighScores] = useState<HighScores | null>(null);
  const [showScores, setShowScores] = useState(false);
  const [playerName, setPlayerName] = useState<string>(localStorage.getItem('playerName') || '');
  const [currentDifficulty, setCurrentDifficulty] = useState<Difficulty>(Difficulty.MEDIUM);
  const [finalResult, setFinalResult] = useState<GameResult | null>(null);
  const [isPathVisible, setIsPathVisible] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [currentRelationshipPath, setCurrentRelationshipPath] = useState<RelationshipStep[] | undefined>();
  const [feedbackSent, setFeedbackSent] = useState(false);

  const gameAreaRef = useRef<HTMLDivElement>(null);
  const relationshipPathForGraph = useMemo(() => {
    console.log('🔍 Debug relationshipPathForGraph:', {
      showResult,
      currentRelationshipPath: currentRelationshipPath?.length || 0,
      questionRelationshipPath: currentQuestion?.relationshipPath?.length || 0,
      currentQuestion: currentQuestion?.id
    });
    
    // Eğer sunucudan gelen detaylı bir yol varsa (cevap sonrası) onu kullan
    if (showResult && currentRelationshipPath && currentRelationshipPath.length > 0) {
      console.log('✅ Using currentRelationshipPath:', currentRelationshipPath);
      return currentRelationshipPath;
    }

    // Backend'den gelen relationshipPath varsa onu kullan
    if (currentQuestion?.relationshipPath && currentQuestion.relationshipPath.length > 0) {
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
  }, [currentQuestion, currentRelationshipPath, showResult, lastAnswerResponse, t]);

  // Layout yönünü otomatik seç: 3+ kişi için LR, az kişi için TB
  const optimalLayoutDirection = useMemo(() => {
    const pathLength = relationshipPathForGraph?.length || 0;
    return pathLength >= 3 ? 'LR' : 'TB';
  }, [relationshipPathForGraph]);

  const person1InfoToDisplay = currentQuestion?.person1Info || null;
  const person2InfoToDisplay = currentQuestion?.person2Info || null;
  const person1 = currentQuestion?.person1Info?.fullName || '...';
  const person2 = currentQuestion?.person2Info?.fullName || '...';
  
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
    fetchHighScores();
  }, [fetchHighScores]);

  const resetTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setTimeRemaining(currentQuestion?.timeLimit || 30);
  };
  
  useEffect(() => {
    if (gameStarted && !gameOver && !showResult && timeRemaining > 0) {
      timerRef.current = setInterval(() => {
        setTimeRemaining((prev) => {
          if (prev <= 1) {
            handleTimerEnd();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else if (timeRemaining === 0 && gameStarted && !gameOver && !showResult) {
      checkAnswer(''); // Süre dolunca boş cevapla kontrol et
    }
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [gameStarted, gameOver, showResult, timeRemaining]);


  const handleTimerEnd = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    if (currentQuestion && !showResult) {
      checkAnswer('');
    }
  };

  const handleNextQuestion = (nextQuestion?: GameQuestion, result?: AnswerResponse) => {
    setShowResult(false);
    setSelectedAnswer('');
    setIsCorrect(false);
    setLastAnswerResponse(null);
    setShowHint(false);
    setFeedbackSent(false);
    if (gameOver) {
      console.info('Game is over, not fetching next question.');
      return;
    }
    
    const questionToUse = nextQuestion || result?.nextQuestion;
    if (questionToUse) {
        setCurrentQuestionSafely(questionToUse);
        resetTimer();
    } else {
        fetchNextQuestion();
    }
  };
  
  const fetchNextQuestion = async () => {
    if (gameOver) return;
    setLoading(true);
    setError(null);
    setShowResult(false);
    setSelectedAnswer('');
    setLastAnswerResponse(null);
    setCurrentRelationshipPath(undefined);
    setShowHint(false);

    try {
      const responseData = await getQuestion(currentDifficulty, i18n.language);
      setCurrentQuestionSafely(responseData);
      if (responseData) {
        setAskedQuestions(prev => [...prev, responseData.id]);
        setTotalQuestions(prev => prev + 1);
      } else {
        await handleGameOver();
      }
    } catch (err: any) {
      if (err instanceof Error) setError(err.message);
      else setError(String(err));
      setCurrentQuestionSafely(null);
    } finally {
      setLoading(false);
      resetTimer();
    }
  };

  const startGame = async () => {
    if (!playerName.trim()) {
      setError(t('player_name.required'));
      toast.error(t('player_name.required'));
      return;
    }
    setLoading(true);
    setError(null);
    await startGameSessionApi(playerName, currentDifficulty, i18n.language);
    setLoading(false);
  };

  useEffect(() => {
    setGameStarted(isGameSessionActive);
  }, [isGameSessionActive]);

  const checkAnswer = async (answer: string) => {
    if (!currentQuestion) {
      setError(t('errors.no_current_question'));
      console.error("checkAnswer, mevcut bir soru olmadan çağrıldı.");
      setLoading(false);
      return;
    }

    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    
    setLoading(true);
    setSelectedAnswer(answer);

    const answerDetails: GameAnswer = {
      sessionId: localStorage.getItem('gameSessionId') || '',
      questionId: currentQuestion.id,
      answer: answer,
      difficulty: currentDifficulty,
      playerName: playerName,
      timeTakenInSeconds: currentQuestion?.timeLimit ? (currentQuestion.timeLimit - timeRemaining) : 0,
      currentScore: score,
      currentStreak: currentStreak,
      gameQuestionCount: totalQuestions,
      correctAnswersCount: correctAnswers,
    };
    
    try {
      const result = await submitAnswer(answerDetails, i18n.language);
      setLastAnswerResponse(result);
      setIsCorrect(result.correctAnswer);
      setScore(result.updatedScore);
      setTotalQuestions(prev => prev + 1);
      
      if (result.correctAnswer) {
        setCorrectAnswers(prev => prev + 1);
        const newStreak = result.updatedStreak;
        setCurrentStreak(newStreak);
        if (newStreak > maxStreak) {
          setMaxStreak(newStreak);
        }
      } else {
        setCurrentStreak(0);
      }
      
      if(result.relationshipPath) {
        setCurrentRelationshipPath(result.relationshipPath);
      }
      
      setShowResult(true);

      if (result.gameOver) {
        setGameOver(true);
        handleGameOver(result.finalResult);
      }

    } catch (err: any) {
      setError(err.message || t('errors.answer_submission_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleFeedback = async (feedback: 'good' | 'bad') => {
    if (!currentQuestion || !lastAnswerResponse) return;

    const feedbackData: GameQuestionFeedbackDTO = {
      questionId: currentQuestion.id,
      relationshipType: currentQuestion.relationshipType,
      isCorrect: lastAnswerResponse.correctAnswer,
      feedback: feedback
    };

    setFeedbackSent(true);
    await sendFeedback(feedbackData);
  };

  const restartGame = () => {
    setGameStarted(false);
    setGameOver(false);
    setScore(0);
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
    setIsPathVisible(false);
    setLastAnswerResponse(null);
    setCurrentRelationshipPath(undefined);
    setFeedbackSent(false);
    
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
  };

  const formatDifficulty = (diff: string): string => {
    return t(`difficulty.${diff.toLowerCase()}` as any, { defaultValue: diff });
  };

  const toggleShowPath = () => {
    setIsPathVisible(prev => !prev);
  };

  const toggleHint = () => {
    setShowHint(!showHint);
  };

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
        <Typography variant="h6" sx={{ mb: 1 }}>{final.playerName}, {t('game_over.score')}: {final.score}</Typography>
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

  const handleGameOver = async (finalResultParam?: GameResult | null) => {
    setGameOver(true);
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    // Eğer backend'den final result gelmemişse, frontend'deki verilerle oluştur
    const resultToSave = finalResultParam || {
      playerName: playerName,
      score: score,
      difficulty: currentDifficulty,
      correctAnswers: correctAnswers,
      totalQuestions: totalQuestions,
      maxStreak: maxStreak,
      date: new Date().toISOString().split('T')[0]
    };

    setFinalResult(resultToSave);
    
    try {
      await recordGameResult({ ...resultToSave }, i18n.language);
      fetchHighScores();
    } catch (err) {
      console.error("Skor kaydedilirken hata:", err);
    }
  };

  useEffect(() => {
    if (!gameStarted) {
      const lastPlayerName = localStorage.getItem('playerName');
      if (lastPlayerName) {
        setPlayerName(lastPlayerName);
      }
    }
  }, [gameStarted]);

  const setCurrentQuestionSafely = (questionData: GameQuestion | null) => {
    if (questionData) {
      const safeQuestion: GameQuestion = {
        ...questionData,
        id: questionData.id || `question-${Date.now()}`,
        options: questionData.options || [],
        person1Info: questionData.person1Info,
        person2Info: questionData.person2Info,
        relationshipPath: questionData.relationshipPath
      };
      setCurrentQuestion(safeQuestion);
    } else {
      setCurrentQuestion(null);
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
    // Eğer oyun başlamışsa yeni dilde devam etmek için sorunu yeniden al
    if (gameStarted && currentQuestion) {
      fetchNextQuestion();
    }
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
    return <ErrorMessage message={error} />;
  }
  
  if (!currentQuestion) {
    return <ErrorMessage message={t('error.question_load')} />;
  }

  const { options, timeLimit, difficulty: questionDifficulty } = currentQuestion;
  const timeProgress = (timeLimit || 30) > 0 ? (timeRemaining / (timeLimit || 30)) * 100 : 0;

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
                label={`${t('game.score')}: ${score}`} 
                color="primary" 
                variant="filled" 
              />
              <Chip 
                icon={<TimerIcon />} 
                label={`${t('streak')}: ${currentStreak}`} 
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
                    backgroundColor: timeRemaining < 10 ? theme.palette.error.main : timeRemaining < 20 ? theme.palette.warning.main : theme.palette.secondary.main, 
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
                {t('game.question_title', { number: totalQuestions })}
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
                {options.map((option, index) => (
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
                    onClick={() => handleNextQuestion(lastAnswerResponse.nextQuestion, lastAnswerResponse)}
                    sx={{ mt: 2 }}
                    disabled={gameOver}
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
                height: '350px', // Sabit yükseklik
                minHeight: '350px',
                maxHeight: '500px', // Maksimum yükseklik
                '& > *': { width: '100%', height: '100%' } // Child elementlere boyut veriyoruz
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