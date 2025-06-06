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
  personDeathYear?: number;
  relationshipToNextPerson?: string;
  sourcePerson?: boolean;
  targetPerson?: boolean;
}

export interface GameQuestion {
  id: string;
  questionText: string;
  options: string[];
  difficulty: Difficulty;
  person1Info?: PersonInfo;
  person2Info?: PersonInfo;
  timeLimit?: number;
  relationshipPath?: RelationshipStep[];
  correctAnswer?: string;
}

export interface InitialGameData {
  firstQuestion: GameQuestion;
  playerName: string;
  difficulty: Difficulty;
}

export interface GameSession {
  playerName: string;
  difficulty: Difficulty;
  currentQuestion?: GameQuestion | null;
  currentScore: number;
  currentStreak: number;
  questionsAnswered: number;
  correctAnswersCount: number;
  totalQuestionsInGame: number;
  askedQuestionSignaturesInThisGame: string[];
  isGameOver: boolean;
  finalResult?: GameResult | null;
}

export interface GameAnswer {
  questionId: string;
  answer: string;
  timeTakenInSeconds: number;
  difficulty: Difficulty;
  playerName: string;
  askedQuestionSignaturesInThisGame: string[];
  currentScore: number;
  currentStreak: number;
  gameQuestionCount: number;
  correctAnswersCount: number;
  sessionId?: string;
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
  id?: string;
  playerName: string;
  score: number;
  difficulty: Difficulty;
  correctAnswers: number;
  totalQuestions: number;
  maxStreak: number;
  date?: string;
  gameOver?: boolean;
  badges?: string[];
  isHighScore?: boolean;
}

export interface HighScores {
  [Difficulty.EASY]: GameResult[];
  [Difficulty.MEDIUM]: GameResult[];
  [Difficulty.HARD]: GameResult[];
}

export interface RecordScoreRequest {
  playerName: string;
  score: number;
  difficulty: Difficulty;
  correctAnswers: number;
  totalQuestions: number;
  maxStreak: number;
} 