export enum Difficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD'
}

export interface StartGameRequest {
  playerName: string;
  difficulty: Difficulty;
}

export interface PersonInfo {
  id: string | number;
  fullName: string;
  gender?: string;
  birthYear?: number | null;
  deathYear?: number | null;
}

export interface RelationshipStep {
  personId: number | string;
  personName: string;
  personGender?: 'Erkek' | 'Kadın' | string;
  personBirthYear?: number;
  relationshipToNextPerson?: string;
  sourcePerson?: boolean;
  targetPerson?: boolean;
}

export interface GameQuestion {
  id: string;
  person1: string;
  person2: string;
  options: string[];
  correctAnswer?: string;
  difficulty: Difficulty;
  timeLimit?: number;
  person1Info?: PersonInfo;
  person2Info?: PersonInfo;
  relationshipPath?: RelationshipStep[];
}

export interface GameSession {
  sessionId: string;
  playerName?: string;
  currentQuestion?: GameQuestion | null;
  questionsAnswered?: number;
  totalQuestionsInSession?: number;
  currentScore?: number;
  currentStreak?: number;
  difficulty: Difficulty;
  gameOver: boolean;
  finalResult?: GameResult | null;
}

export interface GameAnswer {
  sessionId: string;
  questionId: string;
  answer: string;
  timeTakenInSeconds?: number;
}

export interface AnswerResponse {
  correctAnswer: boolean;
  correctAnswerText?: string;
  pointsEarned: number;
  updatedScore: number;
  updatedStreak: number;
  nextQuestion?: GameQuestion;
  gameOver: boolean;
  finalResult?: GameResult;
  relationshipPath?: RelationshipStep[];
}

export interface GameResult {
  playerName: string;
  score: number;
  difficulty: Difficulty;
  correctAnswers: number;
  totalQuestions: number;
  maxStreak: number;
  gameOver: boolean;
  badges?: string[];
  isHighScore?: boolean;
  correct?: boolean;
  message?: string;
  askedQuestionSignaturesInThisGame?: string[];
}

export interface HighScores {
  [Difficulty.EASY]: GameResult[];
  [Difficulty.MEDIUM]: GameResult[];
  [Difficulty.HARD]: GameResult[];
} 