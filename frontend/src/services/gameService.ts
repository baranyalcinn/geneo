import { apiService } from './apiService';
import {
  Difficulty,
  StartGameRequest,
  InitialGameData,
  HighScores,
  GameQuestion as GameQuestionType,
  GameAnswer as GameAnswerType,
  AnswerResponse as AnswerResponseType,
  GameResult as GameResultType,
  RecordScoreRequest
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
 * @returns A promise that resolves to the initial game data.
 */
export const startGame = async (playerName: string, difficulty: Difficulty): Promise<InitialGameData> => {
  const requestBody: StartGameRequest = { playerName, difficulty };
  return apiService.post<InitialGameData>('game/start', requestBody);
};

/**
 * Ends the current game session. This function might be deprecated if game end is handled by submitAnswer or recordGameResult.
 * For now, assuming it might still be used for explicitly ending a game prematurely.
 * @param sessionId The ID of the game session to end. (Note: Backend doesn't seem to use sessionId actively in provided code)
 * @returns A promise that resolves to the game result, conforming to the GameResult type from ../types/game.
 */
export const endGame = async (sessionId: string): Promise<GameResultType> => {
  console.warn("endGame service function called, but backend implementation might be missing or different.");
  return Promise.reject(new Error("endGame endpoint functionality needs review with backend."));
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
    const scoresFromApi = await apiService.get<{[key in Difficulty]: any[]}>('game/highscores');
    
    if (!scoresFromApi || typeof scoresFromApi !== 'object') {
      console.warn("API'den geçersiz yüksek skor verisi alındı, varsayılan kullanılıyor.", scoresFromApi);
      return defaultScores;
    }
    
    const formattedScores: HighScores = { ...defaultScores };
    for (const diffKey of Object.values(Difficulty)) {
        if (scoresFromApi[diffKey] && Array.isArray(scoresFromApi[diffKey])) {
            formattedScores[diffKey] = scoresFromApi[diffKey].map(score => ({
                ...score,
                date: score.date ? score.date.toString() : undefined
            } as GameResultType));
        } else {
            formattedScores[diffKey] = [];
        }
    }
    localStorage.setItem('highScores', JSON.stringify(formattedScores));
    return formattedScores;
  } catch (error) {
    console.error('Yüksek skorlar API'den alınırken hata oluştu.', error);
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
 * Fetches a question based on difficulty. (This is for ad-hoc question generation, not typically during a game flow)
 * @param difficulty The difficulty level for the question.
 * @returns A promise that resolves to a question conforming to the GameQuestionType type.
 */
export const getQuestion = async (difficulty: Difficulty): Promise<GameQuestionType> => {
  try {
    const questionData = await apiService.get<GameQuestionType>('game/question', { params: { difficulty } });
    
    if (questionData) {
      return {
        ...questionData,
        id: questionData.id || `adhoc-question-${Date.now()}`,
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
 * @param answerDetails The answer to submit, conforming to the GameAnswerType.
 * @returns A promise that resolves to the result of the answer, conforming to the AnswerResponseType.
 */
export const submitAnswer = async (answerDetails: GameAnswerType): Promise<AnswerResponseType> => {
  return apiService.post<AnswerResponseType>('game/answer', answerDetails);
};

/**
 * Records the game result to the database when a game is completed.
 * @param scoreDetails The game result object, conforming to RecordScoreRequest.
 * @returns A promise that resolves to the recorded game result, of type GameResultType.
 */
export const recordGameResult = async (scoreDetails: RecordScoreRequest): Promise<GameResultType> => {
  try {
    const result = await apiService.post<GameResultType>('game/record-score', scoreDetails);
    return result;
  } catch (error) {
     console.error("Oyun sonucu kaydedilirken hata oluştu:", error);
     throw error;
  }
};