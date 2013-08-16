using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace TaskExtensions
{
    public interface IDelayed : IComparable<IDelayed>
    {
        TimeSpan GetRemainingDelay();
    }
}
