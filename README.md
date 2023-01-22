# pseudoLanguage
Program to generate a set of words for a fictional language.  
Word construction uses a kind of finite state machine (FSM) where pieces of a word are put together according to a pattern
defined by the language's state transition table (STT).
Every row in the STT represents a node in the FSM, and is constructed by declaring values in three semicolon-separated columns:
- input node, which works as a reference key.
  - nodes are named ``S`` if they are potential starting points, or ``N`` for intermediate nodes. 
  - as node names must be unique, the ``[S|N]`` is generally followed by a numeric value.  
- build step, indicating in what way that node will contribute to the word being constructed.
  - ``_`` means that the node does nothing.
  - characters will be appended to the string being constructed  
  - multiple valid options can be declared by separating them with a pipe ``|``
  - which option that is selected is random, but can be weighted by adding a numeric value in parentheses next to the characters.
    - for example ``a(1) | e(5)`` means that the odds of the node adding `e` to a word instead of `a` is 5:1
- links, indicating where the process can go next by declaring one or more nodes by their reference key
  - as with the build step, multiple choices can be declared by separating them with a pipe ``|`` , and selection can be 
weighted by a value in parentheses next to the node in question.
  - a node that can terminate would add `O` to its links to indicate that the process may end at this point. 

The resource folder holds some CSV-files with examples of STTs, which are implemented in the tests.   
Might require some touch-up and cleaning but more or less a finished project.   

## Potential expansions/features
Assigning actual meaning to these words is an interesting idea but outside the scope of this project as it was only intended to deal with syntax, not semantics.   

## TODOs:
- Add a batch file for drag-drop of STT file and generate an output file with words, and a potentially a log file for debug info.
