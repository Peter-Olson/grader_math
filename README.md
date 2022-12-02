# grader_math
A program that grades a student's answers stored in a text file based on a solution text file

### Response and Solutions Text File Formatting

The Grader program allows for a higher degree of mathematical flexibility for automating the grading process for an assignment.
Unlike online software solutions like 'Savvas', this program allows for multiple answers to be accepted, including the ability
for giving out partial credit in various ways. Specifically, the program allows for the following properties to be controlled:

1. Multiple accepted solutions
2. Partial credit
3. A range of acceptable solutions
4. Comments based on responses
5. Equivalent solutions
6. Staggered partial credit ranges
7. User-response clean-up
8. Mathematical equivalence checking
9. Fractional equivalence checking
10. Algebraic equivalence checking
11. List equivalence, order sequence-independent

Various punctuation are used as delimiters for separating tags and solutions. A solution with no tag receives full credit. Below are the delimiters that are used:

* & separates alternate solutions
* ; separates tags attached to solution values
* | separates equivalent solutions, which can be attached to the same tags

Below is a list of the tags that are being used:

* Partial credit tag - A singular decimal between 0.0 and 1.0 should be used
* Comment tag - Any non-numeric sequence counts as a comment, so long as it does not fit into the range tag
* Range tag - There are two types of range tags:
  * Range 5.0 - This tag would accept all values within 5.0 of the solution value
  * Range 5.0 to 10.0 - This tag would accept all values 5.0 from the solution value, but less than 10.0 from the solution value.
                        Useful for partial credit ranges

Below is an example response text file (on the left) and the solution text file on the right

Student file format examples:           Solution file format examples:
    1. 23                                   1. 23 & 23.0 & 92;Multiplied by 2 instead of dividing;0.0 & 17;Subtracted instead of added;0.5
    2. Rectangle                            2. Rectangle & Rect & Parallelogram & Paralelogram & Square; Can't be a square because side C and side B are longer than A and D; 0.5
    3. 4/5                                  3. 4/5 & 0.8
    4. (4, 5)                               4. (4, 5) & (4,5) & 4, 5 & 4,5 & x = 4, y = 5 & 5, 4 | (5, 4) ; Values are switched! ; 0.5 & (-4, -5) | -4, -5 ; x and y must be positive in order to make the left side equal to zero ; 0.5
    5. 342.56                               5. 342.57 ; Range 1.0 & 342.57 ; Range 1.0 to 5.0 ; 0.5 ; Didn't multiply by acceleration?
