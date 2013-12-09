using System;
using System.Collections.Generic;

namespace Clr.Util.Concurrent.Generic
{
    /// <summary>
    /// Basic priority queue based on a heap.  Sorted either by given comparision function or by the natural (IComparable) ordering of elements.
    /// </summary>
    /// <remarks>
    /// Only the absolute minimum interface is implemented.  We are only trying to support the clojure.core.async needs for a DelayQueue.
    /// </remarks>
    public class PriorityQueue<T> where T:IComparable<T>
    {
        #region Data

        const int DefaultInitialCapacity = 11;  // Because that's what OpenJKD has. So, there.

        protected T[] _heap;
        protected IComparer<T> _comparator;
        protected int _count;

        #endregion

        #region C-tors and factories

        public PriorityQueue(int initialCapacity, IComparer<T> comparator)
        {
            if (initialCapacity < 1)
                throw new ArgumentOutOfRangeException("initialCapacity");
            _count = 0;
            _heap = new T[initialCapacity];
            _comparator = comparator;
        }

        public PriorityQueue(int initialCapacity)
            : this(initialCapacity, null)
        {
        }

        public PriorityQueue()
            : this(DefaultInitialCapacity, null)
        {
        }

        #endregion

        #region Interface

        public int Count { get { return _count; } }

        public void Push(T x)
        {
            if (x == null)
                throw new ArgumentNullException("x");
            int i = _count;
            MaybeGrow(i+1);
            ++_count;
            _heap[i] = x;
            if (i > 0)
                Up(i);
        }

        public T Pop()
        {
            if (_count == 0)
                return default(T);

            T val = _heap[0];
            int lastIndex = --_count;
            _heap[0] = _heap[lastIndex];
            _heap[lastIndex] = default(T);
            if (lastIndex != 0)
                Down(0, _count);
            return val;
        }

        public T Peek()
        {
            if (_count == 0)
                return default(T);

            return _heap[0];
        }

        #endregion

        #region Implementation

        // If we were dealing with really large heaps, it would make sense to make all code with comparisons have two branches,
        // one where there is a _comparator, one where there is not.
        // I'm not that ambitious today.
        bool Less(int i, int j)
        {
            if (_comparator != null)
            {
                return _comparator.Compare(_heap[i], _heap[j]) < 0;
            }

            return _heap[i].CompareTo(_heap[j]) < 0;
        }

        void Swap(int i, int j)
        {
            T temp = _heap[i];
            _heap[i] = _heap[j];
            _heap[j] = temp;
        }

        void Up(int j)
        {
            for (; ; )
            {
                int i = (j - 1) / 2;
                if (i == j || !Less(j, i))
                    break;
                Swap(i, j);
                j = i;
            }
        }

        void Down(int i, int n)
        {
            for (; ; )
            {
                int j1 = 2 * i + 1;
                if (j1 >= n || j1 < 0)
                    break;
                int j = j1;
                int j2 = j1 + 1;
                if (j2 < n && !Less(j1, j2))
                    j = j2;
                if  (! Less(j,i))
                    break;
                Swap(i,j);
                i=j;
            }
        }

        void Heapify()
        {
            int n = _count;
            for (int i = n / 2 - 1; i >= 0; i--)
                Down(i, _count);
        }

        void MaybeGrow(int minCap)
        {
            if (minCap < 0)
                throw new ArgumentOutOfRangeException("minimumCapacity");

            int oldCap = _heap.Length;

            if (minCap <= oldCap)
                return;
            
            // Following OpenJDK here on growth strategy:
            // Double if small, increase 50% otherwise.
            // Watch out for overflow

            int newCap =
                oldCap < 64
                ? (oldCap + 1) * 2
                : (oldCap / 2) * 3;
            if (newCap < 0) // overflow
                newCap = Int32.MaxValue;
            if (newCap < minCap)
                newCap = minCap;
            T[] newHeap = new T[newCap];
            Array.Copy(_heap, newHeap, _count);
            _heap = newHeap;
        }

        #endregion
    }
}

