import { apiService } from './apiService';
import {
  Difficulty,
  StartGameRequest,
  GameSession,
  HighScores,
  GameQuestion as GameQuestionType,
  GameAnswer as GameAnswerType,
  AnswerResponse as AnswerResponseType,
  GameResult as GameResultType,
  PersonInfo as PersonInfoType
} from '../types/game';

export interface PersonInfo {
  id: number | string;
  fullName: string;
  gender?: string;
  birthYear?: number;
  age?: number;
  deathYear?: number;
}

export interface RelationshipStep {
  personId: number;
  personName: string;
  personGender?: string;
  personBirthYear?: number;
  relationshipToNextPerson?: string;
  sourcePerson: boolean;
  targetPerson: boolean;
}

export interface GameQuestion {
  id: string;
  person1: string;
  person2: string;
  options: string[];
  correctAnswer: string;
  difficulty: Difficulty;
  timeLimit: number;
  person1Info?: PersonInfo;
  person2Info?: PersonInfo;
  relationshipPath?: RelationshipStep[];
}

export interface GameAnswer {
  answer: string;
  questionId: string;
  difficulty: Difficulty;
  playerName: string;
  sessionId?: string;
  timeTakenInSeconds?: number;
  askedQuestionSignaturesInThisGame?: string[];
  currentScore?: number;
  currentStreak?: number;
}

export interface AnswerResponse {
  correctAnswer: boolean;
  correctAnswerText?: string;
  pointsEarned: number;
  updatedScore: number;
  updatedStreak: number;
  nextQuestion?: GameQuestion;
  gameOver: boolean;
  finalResult?: GameResultType;
  relationshipPath?: RelationshipStep[];
}

// Game service functions

/**
 * Starts a new game session.
 * @param playerName The name of the player.
 * @param difficulty The selected game difficulty.
 * @returns A promise that resolves to the initial game session state.
 */
export const startGame = async (playerName: string, difficulty: Difficulty): Promise<GameSession> => {
  const requestBody: StartGameRequest = { playerName, difficulty };
  return apiService.post<GameSession>('game/start', requestBody);
};

/**
 * Ends the current game session.
 * @param sessionId The ID of the game session to end.
 * @returns A promise that resolves to the game result, conforming to the GameResult type from ../types/game.
 */
export const endGame = async (sessionId: string): Promise<GameResultType> => {
  return apiService.post<GameResultType>(`game/end/${sessionId}`, {});
};

/**
 * Fetches the high scores for all difficulties.
 * @returns A promise that resolves to the high scores map, conforming to the HighScores type from ../types/game.
 */
export const getHighScores = async (): Promise<HighScores> => {
  const defaultScores: HighScores = {
    [Difficulty.EASY]: [],
    [Difficulty.MEDIUM]: [],
    [Difficulty.HARD]: []
  };
  
  try {
    const scores = await apiService.get<HighScores>('game/highscores');
    
    if (!scores || typeof scores !== 'object') {
      console.warn("API'den geçersiz yüksek skor verisi alındı, varsayılan kullanılıyor.", scores);
      return defaultScores;
    }
    
    const formattedScores: HighScores = { ...defaultScores };
    for (const diffKey of Object.values(Difficulty)) {
        if (scores[diffKey] && Array.isArray(scores[diffKey])) {
            formattedScores[diffKey] = scores[diffKey].map(score => score as GameResultType);
        } else {
            formattedScores[diffKey] = [];
        }
    }
    localStorage.setItem('highScores', JSON.stringify(formattedScores));
    return formattedScores;
  } catch (error) {
    console.error('Yüksek skorlar API\'den alınırken hata oluştu.', error);
    try {
      const localScores = localStorage.getItem('highScores');
      if (localScores) {
        console.log("API hatası nedeniyle yerel depodan yüksek skorlar alınıyor.");
        return JSON.parse(localScores) as HighScores;
      }
    } catch (localError) {
      console.error("Yerel depodan skorlar alınırken hata:", localError);
    }
    return defaultScores;
  }
};

/**
 * Fetches a question based on difficulty.
 * @param difficulty The difficulty level for the question.
 * @returns A promise that resolves to a question conforming to the GameQuestion type.
 */
export const getQuestion = async (difficulty: Difficulty): Promise<GameQuestionType> => {
  try {
    console.log(`Soru getiriliyor - Zorluk: ${difficulty}`);
    // Backend'e doğru parametre formatıyla istek yap
    const questionData = await apiService.get<GameQuestionType>(`game/question`, { difficulty });
    
    if (questionData) {
      console.log("API'den soru alındı:", questionData);
      return {
        ...questionData,
        id: questionData.id || `question-${Date.now()}`,
      };
    }
    throw new Error("Sunucudan geçerli soru alınamadı (getQuestion servisi).");
  } catch (error) {
    console.error("Soru alınırken hata:", error);
    throw error;
  }
};

/**
 * Submits an answer to a question and gets the result.
 * @param answer The answer to submit, conforming to the GameAnswer type.
 * @returns A promise that resolves to the result of the answer, conforming to the GameResult type.
 */
export const submitAnswer = async (answer: GameAnswerType): Promise<AnswerResponseType> => {
  return apiService.post<AnswerResponseType>('game/answer', answer);
};

/**
 * Records the game result to the database.
 * @param gameResult The game result object, expected to be of type GameResultType.
 * @returns A promise that resolves to the recorded game result, also of type GameResultType.
 */
export const recordGameResult = async (gameResult: GameResultType): Promise<GameResultType> => {
  // Ensure the endpoint 'game/results' is correct according to your backend API.
  return apiService.post<GameResultType>('game/results', gameResult);
};