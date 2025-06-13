import { apiService } from './apiService';
import {
  Difficulty,
  StartGameRequest,
  InitialGameData,
  HighScores,
  GameQuestion,
  GameAnswer,
  AnswerResponse,
  GameResult,
  RecordScoreRequest,
  GameQuestionFeedbackDTO
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

// Game service functions

/**
 * Starts a new game session.
 * @param playerName The name of the player.
 * @param difficulty The selected game difficulty.
 * @param lang The selected language code (e.g., 'tr', 'en').
 * @returns A promise that resolves to the initial game data.
 */
export const startGame = async (playerName: string, difficulty: Difficulty, lang: string = 'tr'): Promise<InitialGameData> => {
  const requestBody: StartGameRequest = { playerName, difficulty };
  return apiService.post<InitialGameData>('game/start', requestBody, { 
    params: { lang } 
  });
};

/**
 * Starts a new game session without language parameter (uses default 'tr').
 * @param playerName The name of the player.
 * @param difficulty The selected game difficulty.
 * @returns A promise that resolves to the initial game data.
 */
export const startGameSimple = async (playerName: string, difficulty: Difficulty): Promise<InitialGameData> => {
  return startGame(playerName, difficulty, 'tr');
};

/**
 * Ends the current game session. This function might be deprecated if game end is handled by submitAnswer or recordGameResult.
 * For now, assuming it might still be used for explicitly ending a game prematurely.
 * @param sessionId The ID of the game session to end. (Note: Backend doesn't seem to use sessionId actively in provided code)
 * @returns A promise that resolves to the game result, conforming to the GameResult type from ../types/game.
 */
export const endGame = async (sessionId: string): Promise<GameResult> => {
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
            } as GameResult));
        } else {
            formattedScores[diffKey] = [];
        }
    }
    localStorage.setItem('highScores', JSON.stringify(formattedScores));
    return formattedScores;
  } catch (error) {
    console.error("Yüksek skorlar API'den alınırken hata oluştu.", error);
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
 * @param lang The selected language code (e.g., 'tr', 'en').
 * @returns A promise that resolves to a question conforming to the GameQuestionType type.
 */
export const getQuestion = async (difficulty: Difficulty, lang: string): Promise<GameQuestion> => {
  try {
    const questionData = await apiService.get<GameQuestion>('game/question', { 
      params: { difficulty, lang } 
    });
    
    if (questionData) {
      return {
        ...questionData,
        id: questionData.id || `adhoc-question-${Date.now()}`,
      };
    }
    throw new Error("Sunucudan geçerli soru alınamadı");
  } catch (error: any) {
    console.error("Soru alınırken hata:", error);
    
    // Backend bağlantı hataları için özel mesajlar
    if (error.code === 'ECONNREFUSED' || error.code === 'ERR_NETWORK') {
      throw new Error("Backend sunucusuna bağlanılamıyor. Sunucunun çalıştığından emin olun.");
    }
    
    if (error.response?.status === 404) {
      throw new Error("Soru servisi bulunamadı. API endpoint'leri kontrol edin.");
    }
    
    if (error.response?.status >= 500) {
      throw new Error("Sunucu hatası. Lütfen daha sonra tekrar deneyin.");
    }
    
    throw new Error(error.message || "Soru alınırken bilinmeyen bir hata oluştu");
  }
};

/**
 * Submits an answer to a question and gets the result.
 * @param answerDetails The answer to submit, conforming to the GameAnswerType.
 * @param lang The selected language code (e.g., 'tr', 'en').
 * @returns A promise that resolves to the result of the answer, conforming to the AnswerResponseType.
 */
export const submitAnswer = async (answerDetails: GameAnswer, lang: string): Promise<AnswerResponse> => {
  return apiService.post<AnswerResponse>('game/answer', answerDetails, { 
    params: { lang } 
  });
};

/**
 * Records the game result to the database when a game is completed.
 * @param scoreDetails The game result object, conforming to RecordScoreRequest.
 * @param lang The selected language code (e.g., 'tr', 'en').
 * @returns A promise that resolves to the recorded game result, of type GameResultType.
 */
export const recordGameResult = async (scoreDetails: RecordScoreRequest, lang: string): Promise<GameResult> => {
  try {
    const result = await apiService.post<GameResult>('game/record-score', scoreDetails, { 
      params: { lang } 
    });
    return result;
  } catch (error) {
     console.error("Oyun sonucu kaydedilirken hata oluştu:", error);
     throw error;
  }
};

/**
 * Sends feedback for a game question.
 * @param feedbackData The feedback data to submit.
 * @returns A promise that resolves when the feedback is successfully sent.
 */
export const sendFeedback = async (feedbackData: GameQuestionFeedbackDTO): Promise<void> => {
  try {
    await apiService.post('game/feedback', feedbackData);
  } catch (error) {
    console.error("Oyun geri bildirimi gönderilirken hata oluştu:", error);
    // Hatanın yutulması, geri bildirim gönderimi kritik bir işlem olmadığı için
    // oyun akışını kesmemek adına tercih edilebilir.
  }
};