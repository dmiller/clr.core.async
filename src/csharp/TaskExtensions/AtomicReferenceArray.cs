using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace TaskExtensions.Generic
{
    /// <summary>
    /// Trivial implementation of java.util.concurrent.atomic.AtomicReferenceArray, really only need get/set
    /// </summary>
    /// <remarks>
    /// See https://code.google.com/p/netconcurrent/source/browse/trunk/src/Spring/Spring.Threading/Threading/AtomicTypes/AtomicReferenceArray.cs for a more idiomatic implementation.
    /// Or see https://github.com/henon/GitSharp/blob/master/GitSharp.Core/Util/AtomicReferenceArray.cs
    /// </remarks>
    [Serializable]
    public class AtomicReferenceArray<T>
    {
        readonly T[] _refArray;
        
        public AtomicReferenceArray(int length) 
        {
            _refArray = new T[length];
        }
    
        public int length { get { return _refArray.Length; } }

        public T this[int index] {
            get {
                lock(this) {
                    return _refArray[index];
                }
            }
            set {
                lock(this) {
                    _refArray[index] = value;
                }
            }
        }

        public T get(int index) 
        {
            return this[index];
        }

        public void set(int index, T newVal)
        {
           this[index] = newVal;
        }

        public bool compareAndSet(int i, T expect, T update)
        {
            lock (this)
            {
                if ( (_refArray[i] == null && expect == null) || (_refArray[i] != null &&  _refArray[i].Equals(expect)))
                {
                    _refArray[i] = update;
                    return true;
                }
                return false;
            }
        }
    }
}

namespace TaskExtensions
{

    public class AtomicReferenceArray : TaskExtensions.Generic.AtomicReferenceArray<Object>
    {
        public AtomicReferenceArray(int length) : base(length)
        {
        }
    }
}
