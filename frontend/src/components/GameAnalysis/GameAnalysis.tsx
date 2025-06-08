import React from 'react';
import { motion } from 'framer-motion';
import { 
  Trophy, 
  Target, 
  Clock, 
  TrendingUp, 
  Award,
  BookOpen,
  BarChart3,
  Zap,
  CheckCircle,
  XCircle
} from 'lucide-react';

interface GameAnalysis {
  sessionId: string;
  playerName: string;
  difficulty: string;
  totalQuestions: number;
  questionsAnswered: number;
  correctAnswers: number;
  finalScore: number;
  maxStreak: number;
  gameStartTime: number;
  gameDuration: number;
  accuracyPercentage: number;
  averageResponseTime: number;
  recommendations: string[];
}

interface GameAnalysisProps {
  analysis: GameAnalysis;
  onClose: () => void;
  onPlayAgain: () => void;
}

export const GameAnalysis: React.FC<GameAnalysisProps> = ({
  analysis,
  onClose,
  onPlayAgain
}) => {
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const remainingSecs = seconds % 60;
    return `${mins}dk ${remainingSecs}sn`;
  };

  const getPerformanceRating = (accuracy: number) => {
    if (accuracy >= 90) return { text: 'Mükemmel', color: 'text-green-600' };
    if (accuracy >= 75) return { text: 'Çok İyi', color: 'text-blue-600' };
    if (accuracy >= 60) return { text: 'İyi', color: 'text-yellow-600' };
    if (accuracy >= 40) return { text: 'Orta', color: 'text-orange-600' };
    return { text: 'Gelişim Gerekli', color: 'text-red-600' };
  };

  const performance = getPerformanceRating(analysis.accuracyPercentage);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
    >
      <motion.div
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.8, opacity: 0 }}
        className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto"
      >
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-6 rounded-t-xl">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold mb-2">
                🎯 Oyun Analizi
              </h2>
              <p className="text-blue-100">
                {analysis.playerName} • {analysis.difficulty} Seviyesi
              </p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold">
                {analysis.finalScore}
              </div>
              <div className="text-sm text-blue-100">Puan</div>
            </div>
          </div>
        </div>

        {/* Ana İstatistikler */}
        <div className="p-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.1 }}
              className="bg-gradient-to-br from-green-50 to-green-100 p-4 rounded-lg text-center"
            >
              <div className="text-2xl">✅</div>
              <div className="text-2xl font-bold text-green-700">
                {analysis.correctAnswers}
              </div>
              <div className="text-sm text-green-600">
                Doğru Cevap
              </div>
            </motion.div>

            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="bg-gradient-to-br from-red-50 to-red-100 p-4 rounded-lg text-center"
            >
              <div className="text-2xl">❌</div>
              <div className="text-2xl font-bold text-red-700">
                {analysis.questionsAnswered - analysis.correctAnswers}
              </div>
              <div className="text-sm text-red-600">
                Yanlış Cevap
              </div>
            </motion.div>

            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.3 }}
              className="bg-gradient-to-br from-blue-50 to-blue-100 p-4 rounded-lg text-center"
            >
              <div className="text-2xl">⏱️</div>
              <div className="text-2xl font-bold text-blue-700">
                {formatDuration(analysis.gameDuration)}
              </div>
              <div className="text-sm text-blue-600">
                Oyun Süresi
              </div>
            </motion.div>

            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="bg-gradient-to-br from-purple-50 to-purple-100 p-4 rounded-lg text-center"
            >
              <div className="text-2xl">🔥</div>
              <div className="text-2xl font-bold text-purple-700">
                {analysis.maxStreak}
              </div>
              <div className="text-sm text-purple-600">
                En Uzun Seri
              </div>
            </motion.div>
          </div>

          {/* Performans Özeti */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.5 }}
            className="bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg p-6 mb-8"
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-gray-800">
                Performans Özeti
              </h3>
              <div className={`flex items-center ${performance.color}`}>
                <span className="font-semibold">{performance.text}</span>
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-600">Doğruluk Oranı</span>
                  <span className="font-semibold">
                    %{analysis.accuracyPercentage.toFixed(1)}
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${analysis.accuracyPercentage}%` }}
                    transition={{ duration: 1, delay: 0.5 }}
                    className="bg-green-500 h-2 rounded-full"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-600">Ortalama Süre</span>
                  <span className="font-semibold">
                    {analysis.averageResponseTime.toFixed(1)}s
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.min((20 - analysis.averageResponseTime) / 20 * 100, 100)}%` }}
                    transition={{ duration: 1, delay: 0.7 }}
                    className="bg-blue-500 h-2 rounded-full"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-600">Tamamlama</span>
                  <span className="font-semibold">
                    {analysis.questionsAnswered}/{analysis.totalQuestions}
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${(analysis.questionsAnswered / analysis.totalQuestions) * 100}%` }}
                    transition={{ duration: 1, delay: 0.9 }}
                    className="bg-purple-500 h-2 rounded-full"
                  />
                </div>
              </div>
            </div>
          </motion.div>

          {/* Öneriler */}
          {analysis.recommendations && analysis.recommendations.length > 0 && (
            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 1.1 }}
              className="bg-gradient-to-r from-yellow-50 to-orange-50 rounded-lg p-6 mb-8"
            >
              <div className="flex items-center mb-4">
                <span className="text-2xl mr-2">💡</span>
                <h3 className="text-xl font-semibold text-gray-800">
                  Gelişim Önerileri
                </h3>
              </div>
              <ul className="space-y-2">
                {analysis.recommendations.map((recommendation, index) => (
                  <motion.li
                    key={index}
                    initial={{ x: -20, opacity: 0 }}
                    animate={{ x: 0, opacity: 1 }}
                    transition={{ delay: 1.2 + index * 0.1 }}
                    className="flex items-start"
                  >
                    <div className="w-2 h-2 bg-orange-400 rounded-full mt-2 mr-3 flex-shrink-0" />
                    <span className="text-gray-700">{recommendation}</span>
                  </motion.li>
                ))}
              </ul>
            </motion.div>
          )}

          {/* Aksiyon Butonları */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 1.5 }}
            className="flex flex-col sm:flex-row gap-4"
          >
            <button
              onClick={onPlayAgain}
              className="flex-1 bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 px-6 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 transition-all duration-200 flex items-center justify-center"
            >
              <span className="mr-2">🏆</span>
              Tekrar Oyna
            </button>
            
            <button
              onClick={onClose}
              className="flex-1 bg-gray-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-gray-700 transition-all duration-200 flex items-center justify-center"
            >
              <span className="mr-2">📊</span>
              Kapat
            </button>
          </motion.div>
        </div>
      </motion.div>
    </motion.div>
  );
}; 