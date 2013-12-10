using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

namespace clojure.core.async
{
    /// <summary>
    /// Minimal implementation of java.util.concurrent.DelayQueue
    /// </summary>
    public class DelayQueue<T> where T:IDelayed
    {
        #region Data

        readonly PriorityQueue<IDelayed> _queue = new PriorityQueue<IDelayed>();

        #endregion

        #region Constructors

        public DelayQueue()
        {
        }

        #endregion

        #region Interface

        public void put(T x)
        {
            offer(x);
        }

        public bool offer(T x)
        {
            lock (this) 
            {
                _queue.Push(x);
                T first = (T)_queue.Peek();
                if ( first == null || ((IDelayed)x).CompareTo((IDelayed)first) < 0)
                    Monitor.PulseAll(this);
            }
            return true;
        }

        public bool offer(T x, TimeSpan duration)
        {
            return offer(x);
        }

        public T peek()
        {
            lock (this)
            {
                T first = (T)_queue.Peek();
                if (first == null || ((IDelayed)first).GetRemainingDelay().Ticks > 0)
                    return default(T);
                else
                {
                    if (_queue.Count != 0)
                        Monitor.PulseAll(this);
                    return first;
                }
            }
        }

        public T poll()
        {
            lock (this)
            {
                T first = (T)_queue.Peek();
                if (first == null || ((IDelayed)first).GetRemainingDelay().Ticks > 0)
                    return default(T);
                else
                {
                    _queue.Pop();
                    if (_queue.Count != 0)
                        Monitor.PulseAll(this);
                    return first;
                }
            }
        }

        public T poll(TimeSpan duration)
        {
            lock (this)
            {
                DateTime deadline = DateTime.Now.Add(duration);
                for (; ; )
                {
                    T first = (T)_queue.Peek();
                    if (first == null)
                    {
                        if (duration.Ticks <= 0)
                            return default(T);
                        Monitor.Wait(this, duration);
                        duration = deadline.Subtract(DateTime.Now);
                    }
                    else
                    {
                        TimeSpan delay = ((IDelayed)first).GetRemainingDelay();
                        if (delay.Ticks > 0)
                        {
                            if (delay > duration)
                                delay = duration;
                            Monitor.Wait(this, delay);
                            duration = deadline.Subtract(DateTime.Now);
                        }
                        else
                        {
                            T x = (T)_queue.Pop();
                            if (_queue.Count != 0)
                                Monitor.PulseAll(this);
                            return x;
                        }
                    }
                }
            }
        }

        public T take()
        {
            lock (this)
            {
                for (; ; )
                {
                    T first = (T)_queue.Peek();
                    if (first == null)
                        Monitor.Wait(this);
                    else
                    {
                        TimeSpan delay = ((IDelayed)first).GetRemainingDelay();
                        if (delay.Ticks > 0)
                            Monitor.Wait(this, delay);
                        else
                        {
                            T x = (T)_queue.Pop();
                            if (_queue.Count != 0)
                                Monitor.PulseAll(this);
                            return x;
                        }
                    }
                }
            }
        }


        public int Count
        {
            get
            {
                lock (this)
                {
                    return _queue.Count;
                }
            }
        }

        #endregion

        #region Implementation
        #endregion
    }


    public class DelayQueue : DelayQueue<IDelayed>
    {
    }
}
