import { GameNews } from '@/types/news';

interface NewsCardProps {
  news: GameNews;
  index: number;
}

export default function NewsCard({ news, index }: NewsCardProps) {
  return (
    <a
      href={news.link}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-xl transition-shadow duration-300 p-6 border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400"
    >
      <div className="flex items-start gap-4">
        <div className="flex-shrink-0">
          <span className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 font-bold text-sm">
            {index + 1}
          </span>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2 flex-wrap">
            <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
              {news.badge}
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-500">
              {news.date}
            </span>
          </div>

          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-2">
            {news.title}
          </h3>

          <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
            {news.summary}
          </p>
        </div>
      </div>
    </a>
  );
}
