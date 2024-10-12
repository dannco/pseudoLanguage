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

## Potential expansions/new features
Assigning actual meaning to these words is an interesting idea but outside the scope of this project as it was only intended to deal with syntax, not semantics.   

Analytics and validation; rather than assigning parameters to indicate when a word has reached a max length, one could simply analyze the language to see that it is fully terminating, ie. does not have any states that loop back into each other. Other validation checks to have in place might be to warn for syntax that is seemingly unpronouncable, like long chains of consonants without vowels. It could be argued that alien/eldritch anatomy could potentially have those kinds of languages, but in those cases the validator might be disabled, or merely in a consulting role.  

Another method of enforcing that a language is terminating is to prevent a word from revisiting an already visited state. That should however be considered a fall-back to avoid getting stuck in endless loops and is, akin the to the maxlength variable being triggered, an indication that the language STT is flawed. 

Reverse engineering a language into a valid STT using sample sentences.  
For example, if a language accepts the words "foo", "bar" and "baz", the process to produce a valid STT should be able to return `S ; foo | bar | baz ; O`, but could also be decomposed further into
```
S ; _ ; N0 | N1
N0 ; f ; N2
N1 ; ba ; N3
N2 ; oo ; O
N3 ; r | z ; O
```

Mutating STTs would be a way to introduce variety and potential for new words to form. Mutation is generally accomplished by decomposing states, and allowing a state to link to a state that did not previously exist in its paths.  
Taking the previous foo|bar|baz example, fully mutating the table without adding any new characters and keeping the language terminating gives
```
S ; _ ; N0 | N1
N0 ; f ; N2 | N4 # new link
N1 ; b ; N4 | N2 # decomposed and new link
N2 ; o ; N5 | N3 # decomposed and new link
N3 ; r | z ; O
N4 ; a ; N3      # new state
N5 ; o ; N3 | N4 # new state
```
Still allowing for the standard words of foo|bar|baz, it has been expanded to allow far|booz|boar|for|faz among others.  
Too much mutation to the language might create non-terminating loops, and so it follows that the mutation alghorithm goes together with a proper analytics and validation process to make sure that any changes do not introduce faults in the language.

## TODOs:
- Add a batch file for drag-drop of STT file and generate an output file with words, and a potentially a log file for debug info.
