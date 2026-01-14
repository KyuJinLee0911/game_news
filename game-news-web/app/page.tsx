import { fetchGameNews } from '@/lib/api';
import NewsList from '@/components/NewsList';

export default async function Home() {
  const news = await fetchGameNews();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            게임 뉴스 TOP 20
          </h1>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            최신 게임 뉴스를 한눈에 확인하세요
          </p>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <NewsList initialNews={news} />
      </main>

      <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <p className="text-center text-sm text-gray-500 dark:text-gray-400">
            데이터는 Google News RSS를 통해 수집됩니다
          </p>
        </div>
      </footer>
    </div>
  );
}
