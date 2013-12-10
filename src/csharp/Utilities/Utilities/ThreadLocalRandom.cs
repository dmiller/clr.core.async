using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

namespace clojure.core.async
{
    public class ThreadLocalRandom : Random
    {
        static private ThreadLocal<ThreadLocalRandom> _currentThreadLocalRandom = new ThreadLocal<ThreadLocalRandom>(() =>
            {
                return new ThreadLocalRandom();
            });

        public static ThreadLocalRandom current { get { return _currentThreadLocalRandom.Value; } }
        public static ThreadLocalRandom Current { get { return _currentThreadLocalRandom.Value; } }


        private ThreadLocalRandom()
            : base()
        {
        }

        private ThreadLocalRandom(int seed)
            : base(seed)
        {
        }


    }
}
