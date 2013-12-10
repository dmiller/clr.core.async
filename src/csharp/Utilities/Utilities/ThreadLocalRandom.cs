/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 *   Author: David Miller
 **/

using System;
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
