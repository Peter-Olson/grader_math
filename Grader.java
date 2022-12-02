
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.*;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.text.DecimalFormat;

/**
   Grader.java
   
   A program that automates grading by checking student solution files
   against a master solution file. Grader allows for the creation of
   automatic responses and response files, for flexibility in grading,
   for optimization in creating solution files and for creating student
   response templates, for pattern recognition of mistakes made across
   questions within an assignment.
   
   @author Peter Olson
   @version 07/22/22 v1.0
*/
public class Grader {

   private static Scanner scanner = new Scanner( System.in );

   private static final String GRADER_SETTINGS_FILE_NAME = "Grader_Settings.txt";
   
   /*Grader Settings -- @@NOTE: Anytime a setting is added, it needs to be added in the setGraderSettings()
        method, the Grader_Settings.txt file, and the global variables below*/
   private static boolean spacesMatter               = false;
   private static boolean useAutoAltSolutions        = true;
   private static int     defaultPointValue          = 1;
   private static boolean manuallyEnterSolutions     = false;
   private static boolean listOrderMatters           = true;
   private static boolean convertFractionsToDecimals = true;
   private static boolean createResultsFile          = true;
   private static boolean includeAlgebraicAlternates = true;

   /**
      Run the menu options
      
      @see printMenu()
      @see setGraderSettings()
      
      @see gradeAllTextFiles()
      @see gradeTextFile()
      @see changeGradingOptions()
      @see createSolutionFile()
      @see editSolutionFile()
      @see createStudentResponseTemplate()
      @see checkForPlagiarism()
      @see generateStatistics()
      @see editAllFiles()
      @see editResponseFile()
      @see printFile()
   */
   public static void main( String[] args ) {

      String response = "";
      printMenu( false );
      do {
         setGraderSettings();
         
         response = scanner.nextLine().toLowerCase();
         char option = response.charAt(0);
         
         switch(option) {
            case 'a': gradeAllTextFiles();             break;
            case 'b': gradeTextFile();                 break;
            case 'c': changeGradingOptions();          break;
            case 'd': createSolutionFile();            break;
            case 'e': editSolutionFile();              break;
            case 'f': createStudentResponseTemplate(); break;
            case 'g': checkForPlagiarism();            break;
            case 'h': generateStatistics();            break;
            case 'i': editAllFiles();                  break;
            case 'j': editResponseFile();              break;
            case 'k': printFile();                     break;
            case 'l': reformatSolutionFile();          break;
            case 'm': retrieveDownloadedFiles();       break;
            
            case 'q': SOPln("\nGoodbye.");             break;
            default : SOPln("\nPlease enter a letter.\n"); break;
         }
         
         if( !response.equals("q") && !response.contains("quit") )
            printMenu( true );
         
      } while( !response.equals("q") && !response.contains("quit") );
   }
   
   /**
      Grade all student response text files by comparing the answers in each
      to the solution text file. The files are graded according to the grading
      options text file and the alternate solutions within the solutions text file.
      
      Any answers found to be incorrect (or if they received partial credit) are
      then listed in a new response text file that includes the grades and points lost
      for that student, along with any automated responses based on the incorrect value
      for each question (these are options and are set within the solutions file).
      
      @see gradeTextFile( File file )
   */
   public static void gradeAllTextFiles() {
      SOPln("\nWhat group of files do you want to edit?\n" +
            "(Enter the identifying name of this group, such as\n" +
            "\"HW6\" or \"Quiz2\"\n");
      String inclusionToken = scanner.nextLine();
   
      File[] studentFiles = getTextFiles( new String[]{"Temp","Settings","Solution"} );
      File solutionFile = getSolutionFile();
      
      //@@DEBUG
      //SOPln("\nThe solution file found is: " + solutionFile.getName() + "\n");
      
      for( File studentFile : studentFiles )
         if( studentFile.getName().toLowerCase().contains( removeWhitespace( inclusionToken.toLowerCase() ) ) )
            gradeTextFile( studentFile, solutionFile );
   }
   
   /**
      Returns the list of text files in the current directory
      
      @return File[] The list of text files in the current directory
   */
   private static File[] getTextFiles() {
      return getTextFiles( new String[]{} );
   }
   
   /**
      Returns the list of text files in the current directory, excluding files whose names contain the given String
      
      @param exclusion The token to check against the names of the Files for the purposes of
                           excluding those files
      @return File[] The list of text files in the current directory
   */
   private static File[] getTextFiles( String exclusion ) {
      return getTextFiles( new String[]{ exclusion } );
   }
   
   /**
      Returns the list of text files in the current directory, excluding files whose names contain any of the tokens
      in the list
           
      @param exclusionList The list of tokens to check against the names of the Files for the purposes of
                           excluding those files
      @return File[] The list of text files in the current directory
   */
   private static File[] getTextFiles( String[] exclusionList ) {
      File dir = new File(".");
      File[] filesList = dir.listFiles();
      ArrayList<File> newFileList = new ArrayList<File>();
      
      for( File file : filesList ) {
         if( file.isFile() ) {
            boolean willInclude = true;
            if( file.toString().contains("txt") && !file.toString().contains("Grader_Settings.txt") ) {
               for( int i = 0; i < exclusionList.length; i++ ) {
                  willInclude &= !file.toString().toLowerCase().contains( exclusionList[i].toLowerCase() );
               }
            } else {
               willInclude = false;
            }
            
            if( willInclude ) newFileList.add( file );
         }
      }
      
      File[] newFileListArray = new File[ newFileList.size() ];
      //convert to array since toArray() does not preserve order
      for( int i = 0; i < newFileList.size(); i++ )
         newFileListArray[i] = newFileList.get(i);
      
      return newFileListArray;
   }
   
   /**
      Gets the correct solution text file.
      
      If the current directory has no text files that contain the
      word 'solution', then 'null' is returned and the user is
      informed that there are no solution files in the current
      directory.
      
      If the current directory has just one solution file, this
      file is automatically returned
      
      If the current directory has multiple solution files, the
      user is asked which solution file that they want to use
      
      @return File The name of the solution text file that will be used
      @see gradeAllTextFiles()
   */
   private static File getSolutionFile() {
      File[] fileList = getTextFiles();
      
      int solutionTextFileCounter = 0;
      int index = -1;
      ArrayList<Integer> solutionFileIndices = new ArrayList<Integer>();
      for( int i = 0; i < fileList.length; i++ ) {
         if( fileList[i].getName().toLowerCase().contains("solution") ) {
            solutionTextFileCounter++;
            index = i;
            solutionFileIndices.add( index );
         }
      }
      
      if( solutionTextFileCounter == 0 ) {
         SOPln("\nNo solution files found.\n");
         return null;
      }
      
      if( solutionTextFileCounter == 1 )
         return fileList[index];
      
      //Slim down file list to just the solution files
      ArrayList<File> solutionFiles = new ArrayList<File>();
      int totalSolutionFiles = solutionFileIndices.size();
      for( int i = 0; i < totalSolutionFiles; i++ )
         solutionFiles.add( fileList[ solutionFileIndices.get(i) ] );
      
      boolean fileFound = false;
      do {
         printFileListByName( solutionFiles );
         SOPln("\nWhich solution file?");
         String response = scanner.nextLine().toLowerCase();
         if( response.equals("quit") ) break;
         
         //If enter number index
         boolean invalidIndex = false;
         if( isNumeric( response.trim().replaceAll("\\.", "" ) ) ) {
            try {
               response = response.trim().replaceAll("\\.", "" );
               int indexPos = Integer.parseInt( response ) - 1;
               return solutionFiles.get( indexPos );
            } catch( NumberFormatException e ) {
               //treat as a name entered, skip exception
            } catch( IndexOutOfBoundsException e ) {
               SOPln("\nInvalid index. Please enter a number between 1 and " + totalSolutionFiles );
               invalidIndex = true;
            }
         }
         
         if( !response.contains(".txt") ) response += ".txt";
         
         //if enter the name of the file ^ v
         if( !invalidIndex )
         for( int i = 0; i < totalSolutionFiles; i++ ) {
            if( response.equals( solutionFiles.get(i).getName().toLowerCase() ) )
               return solutionFiles.get(i);
         }
         
         if( !invalidIndex ) SOPln("\nFile not found. Try again.");
      } while( !fileFound ); //always true if reaches this point
      
      return null; //should not be reached
   }
   
   /**
      Gets the correct student file based on the name
      
      If the current directory has no text files that contain the
      name entered, then 'null' is returned and the user is
      informed that there are no files in the current
      directory that contain that name
      
      If the current directory has just one file that contains this name, this
      file is automatically returned
      
      If the current directory has multiple files that contain this name, the
      user is asked which file that they want to use
      
      @return File The name of the text file that will be used
      @see gradeTextFile()
   */
   private static File getFileByName() {
      File[] fileList = getTextFiles( new String[]{ "solution", "grade" } );
      
      if( fileList.length == 0 ) {
         SOPln("\nNo files found that can be graded.");
         return null;
      }
      
      if( fileList.length == 1 ) {
         return fileList[0];
      }
      
      boolean fileFound = false;
      do {
         printFileListByName( fileList );
         SOPln("\nWhich file?");
         String response = new Scanner(System.in).nextLine().toLowerCase();
         
         //If enter number index
         boolean invalidIndex = false;
         if( isNumeric( response.trim().replaceAll("\\.", "" ) ) ) {
            try {
               response = response.trim().replaceAll("\\.", "" );
               int indexPos = Integer.parseInt( response ) - 1;
               return fileList[ indexPos ];
            } catch( NumberFormatException e ) {
               //treat as a name entered, skip exception
            } catch( IndexOutOfBoundsException e ) {
               SOPln("\nInvalid index. Please enter a number between 1 and " + fileList.length );
               invalidIndex = true;
            }
         }
         
         if( !response.contains(".txt") ) response += ".txt";
         
         //if enter the name of the file ^ v
         if( !invalidIndex )
         for( int i = 0; i < fileList.length; i++ ) {
            if( response.equals( fileList[i].getName().toLowerCase() ) )
               return fileList[i];
         }
         
         if( !invalidIndex ) SOPln("\nFile not found. Try again.");
      } while( !fileFound ); //always true if reaches this point
      
      return null; //should not be reached
   }
   
   /**
      Print the list of text file names in the list
      
      @param fileList The list of text file names to print
      @see getSolutionFile()
      @see printFileListByName( File[] fileList )
   */
   public static void printFileListByName( ArrayList<File> fileList ) {
      int size = fileList.size();
      SOPln();
      for( int i = 0; i < size; i++ ) {
         SOPln( (i+1) + ". " + fileList.get(i).getName() );
      }
   }
   
   /**
      Print the list of text file names in the array
      
      @param fileList The array of text file names to print
      @see getFileByName()
      @see printFileListByName( ArrayList<File> fileList )
   */
   public static void printFileListByName( File[] fileList ) {
      SOPln();
      for( int i = 0; i < fileList.length; i++ )
         SOPln( (i+1) + ". " + fileList[i].getName() );
   }
   
   /**
      Grades the student file by comparing it to the solution file.
      
      This method gathers the files needed and runs the gradeTextFile(...) function
      
      @see getFileByName()
      @see getSolutionFile()
      @see gradeTextFile( File studentFile, File solutionFile )
   */
   public static void gradeTextFile() {
      File studentFile = getFileByName();
      File solutionFile = getSolutionFile();
      
      gradeTextFile( studentFile, solutionFile );
   }
   
   /**
      Grades the student file by comparing it to the solution file.
      
      A response text file is generated that shows the incorrect answers, the student response,
      the automated response, and the total score. This file's name follows the structure of
      
      "NAME_ASSIGNMENT_GRADE.txt"
      
      This method can be optimized by changing the properties of the GRADER_SETTINGS_FILE_NAME file,
      by direct editing or through using the menu options
      
      These settings allow for the grader to grade using absolute or partial credit, adding or
      withholding automated responses, setting extra credit, excluding problems from being graded,
      accepting alternate answers for partial or full credit, accepting solutions within a given
      range, and using scaled partial credit for solutions landing within a given range
      
      @param studentFile The student text file to be graded. The name of the file should follow the format of "NAME_ASSIGNMENT.txt"
      @param solutionFile The solution text file that contains the correct answers. The name of the file should follow the
                          format of "Solutions_ASSIGNMENT.txt"
      @see gradeAllTextFiles()
      @see checkIfFilesAreCompatible( String studentFileName, String solutionFileName )
   */
   private static void gradeTextFile( File studentFile, File solutionFile ) {
      String studentFileName = studentFile.getName();
      String studentName = studentFileName.substring( 0, studentFileName.indexOf("_") );
      String solutionFileName = solutionFile.getName();
      boolean namesMatch = checkIfFilesAreCompatible( studentFileName, solutionFileName );
      
      if( !namesMatch ) {
         SOPln("The files " + studentFileName + " and " + solutionFileName + " are not compatible for grading.\n" +
               "Each file must be a .txt file and have the same assignment name. The solution file must\n" +
               "begin with the word \"solution\" (caps do not matter). The student file must\n" +
               "begin with their name.");
         return;
      }
      
      double totalPoints, maxPoints;
      totalPoints = maxPoints = 0.0;
      Scanner studentScanner = getScanner( studentFile );
      Scanner solutionScanner = getScanner( solutionFile );
      
      /*Student file format examples:           Solution file format examples:
         1. 23                                   1. 23 & 23.0 & 92;Multiplied by 2 instead of dividing;0.0 & 17;Subtracted instead of added;0.5
         2. Rectangle                            2. Rectangle & Rect & Parallelogram & Paralelogram & Square; Can't be a square because side C and side B are longer than A and D; 0.5
         3. 4/5                                  3. 4/5 & 0.8
         4. (4, 5)                               4. (4, 5) & (4,5) & 4, 5 & 4,5 & x = 4, y = 5 & 5, 4 | (5, 4) ; Values are switched! ; 0.5 & (-4, -5) | -4, -5 ; x and y must be positive in order to make the left side equal to zero ; 0.5
         5. 342.56                               5. 342.57 ; Range 1.0 & 342.57 ; Range 1.0 to 5.0 ; 0.5 ; Didn't multiply by acceleration?
      */
      
      String resultsFileText = "";
      String printName = capFirstLetter( studentName ) + ":";
      resultsFileText += printName + "\n";
      SOPln( printName );
      
      int lineNumber = 1;
      ArrayList<String> responseLines = new ArrayList<String>();
      while( studentScanner.hasNextLine() && solutionScanner.hasNextLine() ) {
         //Get relevant text
         String studentLine = studentScanner.nextLine();
         String originalLine = studentLine;
         String solutionLine = solutionScanner.nextLine();
         studentLine = studentLine.substring( studentLine.indexOf(".") + 1, studentLine.length() ).trim();
         solutionLine = solutionLine.substring( solutionLine.indexOf(".") + 1, solutionLine.length() ).trim();
         String problemNumber = originalLine.substring( 0, originalLine.indexOf(".") );
         
         //Handle if total student problems and total solution problems differ
         if( studentLine.isEmpty() ) {
            maxPoints += 1.0;
            continue;
         }
         if( solutionLine.isEmpty() ) {
            SOPln("\nError! The number of solutions for the assignment " + solutionFileName + " is less than the\n" +
                  "number of answers given in the student file " + studentFileName + ".\n" +
                  "\nPlease fix this before grading this file.\n");
            return;
         }
         
         //Formatting
         studentLine = studentLine.toLowerCase();
         studentLine = checkAndConvertToDecimal( studentLine );
         solutionLine = solutionLine.toLowerCase();
         if( !spacesMatter ) studentLine = removeWhitespace( studentLine );
         
         //Save info for response file
         String responseLine = problemNumber + ". ";
         String autoFeedback = "";
         
         String[] solutionParts = solutionLine.split("&");
         
         String firstSolution = findFirstCorrectSolution( solutionParts );
         boolean correctAnswer = false;
         boolean hitCorrectOrPartialAnswer = false;
         for( int i = 0; i < solutionParts.length; i++ ) {

            String[] solutionSpecs = solutionParts[i].trim().split(";");
         
            //No specs
            if( solutionSpecs.length == 1 && studentLine.equals( solutionParts[i] ) ) {
               correctAnswer = true;
               break;
            }
            
            //Handle specs
            double  minRange             = 0.0;
            double  maxRange             = 0.0;
            double  partialCreditRatio   = 1.0;
            double  studentLineValue     = 0.0;
            int     studentLineValueInt  = 0;
            double  solutionLineValue    = 0.0;
            int     solutionLineValueInt = 0;
            boolean doubleFail           = false;
            boolean intFail              = false;
            boolean doubleFailSolution   = false;
            boolean intFailSolution      = false;
            String  solutionValue        = "";
            
            //Parse student answer values
            try {
               studentLineValue = Double.parseDouble( solutionSpecs[0] );
            } catch( NumberFormatException e ) {
               doubleFail = true;
            }
            if( doubleFail ) {
               try {
                  studentLineValueInt = Integer.parseInt( solutionSpecs[0] );
               } catch( NumberFormatException e ) {
                  intFail = true;
               }
            }
            
            //Run through each spec and set settings
            for( int j = 0; j < solutionSpecs.length; j++ ) {
               doubleFailSolution = intFailSolution = false;
            
               solutionSpecs[j] = solutionSpecs[j].trim();
               
               //Check for range tag and set range
               if( solutionSpecs[j].contains("range") ) {
                  String[] rangeSpecs = solutionSpecs[j].split(" ");
                  if( solutionSpecs[j].contains("to") ) {
                     //Set min range
                     try {
                        minRange = Double.parseDouble( rangeSpecs[1] );
                     } catch( NumberFormatException e ) {
                        try {
                           minRange = Integer.parseInt( rangeSpecs[1] );
                        } catch( NumberFormatException f ) {
                           SOPln("\nThis line should not be reached! Search tag: @471");
                           return;
                        }
                     }
                     //Set max range
                     try {
                        maxRange = Double.parseDouble( rangeSpecs[3] );
                     } catch( NumberFormatException e ) {
                        try {
                           maxRange = Integer.parseInt( rangeSpecs[3] );
                        } catch( NumberFormatException f ) {
                           SOPln("\nThis line should not be reached! Search tag: @483");
                           return;
                        }
                     }
                  } else { //no min range
                     try {
                        maxRange = Double.parseDouble( rangeSpecs[1] );
                     } catch( NumberFormatException e ) {
                        try {
                           maxRange = Integer.parseInt( rangeSpecs[1] );
                        } catch( NumberFormatException f ) {
                           SOPln("\nThis line should not be reached! Search tag: @491");
                           return;
                        }
                     }
                  }
                  
                  continue;
               }
               
               //Check for solution value and partial credit ratio
               if( j == 0 || isNumeric( solutionSpecs[j] ) ) {
                  //Parse solution values
                  double tempDouble = 0.0;
                  int tempInt = 0;
                  try {
                     if( j == 0 ) solutionLineValue = Double.parseDouble( solutionSpecs[j] );
                     else         tempDouble = Double.parseDouble( solutionSpecs[j] );
                  } catch( NumberFormatException e ) {
                     doubleFailSolution = true;
                     try {
                        if( j == 0 ) solutionLineValueInt = Integer.parseInt( solutionSpecs[j] );
                        else         tempInt = Integer.parseInt( solutionSpecs[j] );
                     } catch( NumberFormatException f ) {
                        intFailSolution = true;
                     }
                  }
                  
                  boolean isPartialCreditValue = false;
                  
                  //Double value
                  if( !doubleFailSolution && j != 0 )
                     isPartialCreditValue = tempDouble < 1.0 && tempDouble > -1.0;
                  //Int value
                  else if( !intFailSolution && j != 0 )
                     isPartialCreditValue = tempInt < 1 && tempInt > -1;
                  
                  if( isPartialCreditValue )
                     partialCreditRatio = Double.parseDouble( solutionSpecs[j] );
                  else
                     solutionValue = solutionSpecs[j]; //This is a String
                     
                  continue;
               }
               
               //Check for automated response
               if( !isNumeric( solutionSpecs[j] ) ) {
                  autoFeedback = solutionSpecs[j];
                  continue;
               } else {
                  SOPln("Error! Formatting issue within solution file '" + solutionFileName + "'.\n" +
                        "See line #" + lineNumber + ". One or more solution tags does not follow the format of\n" +
                        "Problem#. Solution Value ; Range NUMBER ; Range NUMBER to NUMBER ; PARTIAL CREDIT RATIO\n" +
                        "where the range and partial credit tags are optional. For solution tags that do not have\n" +
                        "the range or partial credit tags, there should be no semicolons present. In that case, the\n" +
                        "solution should look like the following:\n" +
                        "Problem#. Solution Value\n\nPlease edit this line and then run this program again.\n");
                  return;
               }
            }//end for loop for looking through specs seperated by semicolons
            
            /*Check correctness based on solution tags*/
            
            //String response
            if( intFail || intFailSolution ) {
               if( studentLine.equals( solutionValue ) ) {
                  totalPoints += defaultPointValue * partialCreditRatio;
                  if( partialCreditRatio < 1.0 )
                     responseLine += "XXX Partial Credit: " + (defaultPointValue * partialCreditRatio) + "/" + defaultPointValue + ". Student answer: " + studentLine + ", Solution: " + firstSolution.trim()/*(!doubleFailSolution ? solutionLineValue : solutionLineValueInt)*/ + ". " + autoFeedback;
                  else
                     responseLine += "Correct";
                  
                  hitCorrectOrPartialAnswer = true;
                  break;
               //Check to see if solutions are correct, but are just rearranged in different orders.
               //Note that this requires the 'listOrderMatters' setting to be set to false
               } else if( !listOrderMatters && studentLine.contains(",") ) {
                  String[] studentList = studentLine.replaceAll("\\s+","").split(",");
                  String[] solutionList = solutionValue.replaceAll("\\s+","").split(",");
                  if( studentList.length == solutionList.length && studentList.length != 0 && solutionList.length != 0 ) {
                     boolean passed = true;
                     for( int j = 0; j < solutionList.length; j++ ) {
                        if( !solutionLine.contains( studentList[j] ) )
                           passed = false;
                     }
                     
                     //The lists are equal, but possibly rearranged differently
                     if( passed ) {
                        totalPoints += defaultPointValue * partialCreditRatio;
                        if( partialCreditRatio < 1.0 )
                           responseLine += "XXX Partial Credit: " + (defaultPointValue * partialCreditRatio) + "/" + defaultPointValue + ". Student answer: " + studentLine + ", Solution: " + firstSolution.trim()/*(!doubleFailSolution ? solutionLineValue : solutionLineValueInt)*/ + ". " + autoFeedback;
                        else
                           responseLine += "Correct";
                        
                        hitCorrectOrPartialAnswer = true;
                        break;
                     }
                  }
               }
            } else { //For int or double values
               if( Math.abs( studentLineValue - solutionLineValue ) <= maxRange && Math.abs( studentLineValue - solutionLineValue ) >= minRange ) {
                  totalPoints += defaultPointValue * partialCreditRatio;
                  if( partialCreditRatio < 1.0 )
                     responseLine += "XXX Partial Credit: " + (defaultPointValue * partialCreditRatio) + "/" + defaultPointValue + ". Student answer: " + studentLine + ", Solution: " + firstSolution.trim()/*(!doubleFailSolution ? solutionLineValue : solutionLineValueInt)*/ + ". " + autoFeedback;
                  else
                     responseLine += "Correct";
                  
                  hitCorrectOrPartialAnswer = true;
                  break;
               }
            }
            
            autoFeedback = "";
            
         } //end for loop checking all solutions
         
         //Update points
         if( correctAnswer ) {
            totalPoints += 1.0;
            responseLine += "Correct";
         } else if( !hitCorrectOrPartialAnswer )
            responseLine += "XXX Incorrect: 0/" + defaultPointValue + ". Student answer: " + studentLine + ", Solution: " + firstSolution.trim();
         
         maxPoints += defaultPointValue;
         
         lineNumber++;
         
         resultsFileText += responseLine + "\n";
         SOPln( responseLine );
         
      } //end while going through files
      
      String score = "\nScore: " + totalPoints + " / " + maxPoints;
      resultsFileText += score;
      SOPln( score );
      SOPln( "------------------------------------" );
      
      //Write results file
      if( createResultsFile ) {
         String newFileName = studentFileName.substring( 0, studentFileName.indexOf(".") ) + "_Grade.txt";
         createTextFile( newFileName );
         writeToFile( newFileName, resultsFileText );
      }
   }
   
   /**
      Finds and returns the correct solution value, if it exists, for these
      specs, which are delimited by semicolons
      
      @param solutionSpecs The specs for this answer
      @return String If the answer is correct, returns the correct answer. If it
                     is an answer that is incorrect, or would receive partial
                     credit, returns the empty string ""
   */
   private static String findFirstCorrectSolution( String[] solutionParts ) {
      for( int partCount = 0; partCount < solutionParts.length; partCount++ ) {
         String[] parts = solutionParts[partCount].split(";");
         
         boolean skip = false;
         for( int rep = 1; rep < parts.length; rep++ ) {
            if( isNumeric( parts[rep].trim() ) ) {
               skip = true;
               break;
            }
         }
         
         if( skip ) continue;
         
         return parts[0].trim();
      }
      
      return "";
   }
   
   /**
      Checks if the student file and the solution file are compatible.
      
      Checks for the following:
      1) The student file name should be in the format NAME_ASSIGNMENT.txt
      2) The solution file name should be in the format Solution_ASSIGNMENT.txt
      3) Both files end in the same assignment name, followed by .txt
      
      @param studentFileName The name of the student file. Includes the .txt extension
      @param solutionFileName The name of the solution file. Includes the .txt extension
      @return boolean True if the files are compatible, false otherwise
      
      @see gradeTextFile( File studentFile, File solutionFile )
   */
   private static boolean checkIfFilesAreCompatible( String studentFileName, String solutionFileName ) {
      
      //1) The student file name should be in the format NAME_ASSIGNMENT.txt
      String[] tokens = studentFileName.split("_|-");
      if( tokens.length < 2 ) return false;
      if( tokens[0].length() == 0 ) return false;
      String assignmentName = tokens[1].substring(0, tokens[1].length() - 4);
      if( assignmentName.length() == 0 ) return false;
      
      //2) The solution file name should be in the format Solution_ASSIGNMENT.txt
      String[] solutionTokens = solutionFileName.split("_|-");
      if( solutionTokens.length < 2 ) return false;
      if( solutionTokens[0].length() == 0 ) return false;
      if( !solutionTokens[0].toLowerCase().contains("solution") ) return false;
      String solutionAssignmentName = solutionTokens[1].substring(0, solutionTokens[1].length() - 4);
      if( solutionAssignmentName.length() == 0 ) return false;
      
      //3) Both files end in the same assignment name, followed by .txt
      if( !assignmentName.toLowerCase().equals( solutionAssignmentName.toLowerCase() ) ) return false;
      
      return true;
   }
   
   /**
      Change the settings for grading files
   */
   public static void changeGradingOptions() {
      String response = "";
      Scanner fileScanner = null;
      File graderSettingsFile = new File( GRADER_SETTINGS_FILE_NAME );
      String graderSettingsText = getTextFromFile( graderSettingsFile );
      
      int changeOptionNumber = 0;
      boolean runAgain = true;
      do {
         SOPln("\nWhich setting would you like to change? Enter the number to switch the setting\n");
         printSettings( graderSettingsText );

         String option = scanner.nextLine();
         
         if( option.toLowerCase().contains("q") ) {
            runAgain = false;
            continue;
         }
         
         try {
            changeOptionNumber = Integer.parseInt( option ) - 1;
         } catch( NumberFormatException e ) {
            e.printStackTrace();
         }
         
         if( changeOptionNumber >= 0 )
            graderSettingsText = changeSettings( graderSettingsText, changeOptionNumber );
         
         pause();
         
      } while( runAgain );
      
      //Overwrite file with new settings. Will have 0, 1 or more changes
      writeToFile( graderSettingsFile.getPath(), graderSettingsText );
   }
   
   /**
      Change the settings by flipping the boolean on the option selected
      
      @param graderSettingsText The text to change one of the options
      @param changeOptionNumber The line of the settings file to be changed
      @return String The text of file, now with an option that has been changed
   */
   public static String changeSettings( String graderSettingsText, int changeOptionNumber ) {
      String[] lines = graderSettingsText.split("\r?\n|\r");
      
      if( lines.length <= changeOptionNumber ) {
         SOPln("\nInvalid option number. Please enter a number between 1 and " + lines.length + ", inclusive.\n");
         return graderSettingsText;
      }
      
      String[] parts = lines[ changeOptionNumber ].split("\\?");
      
      if( parts[1].toLowerCase().trim().equals("true") )
         lines[ changeOptionNumber ] = parts[0] + "? false";
      else if( parts[1].toLowerCase().trim().equals("false") )
         lines[ changeOptionNumber ] = parts[0] + "? true";
      else {
         SOPln("\nWhat will the new value be?\n");
         lines[ changeOptionNumber ] = parts[0] + "? " + scanner.nextInt() + scanner.nextLine();
      }
      
      String newSettings = "";
      for( int i = 0; i < lines.length; i++ ) {
         if( i != lines.length - 1 ) newSettings += lines[i] + "\n";
         else                        newSettings += lines[i];
      }
      
      return newSettings;
   }
   
   /**
      Print the settings String with formatting
      
      @param graderSettingsText The text of the GraderSettings.txt file
   */
   public static void printSettings( String graderSettingsText ) {
      String[] lines = graderSettingsText.split("\r?\n|\r");

      for( int i = 0; i < lines.length; i++ ) {
         SOPln( (i+1) + ". " + lines[i] );
      }
      
      SOPln("\nEnter Q to quit\n");
   }
   
   /**
      Creates a solution file by I/O with the user
      
      Solution files must begin with the word solution and include a title for the assignment/test.
      Solutions should be separated by &'s. Any solution that is worth partial credit, or solutions with
      set ranges, or solutions with automated responses should be separated by semicolons, where the
      solution appears first. The order for the other settings do not matter. Spaces and capitalization
      also does not matter.
   */
   public static void createSolutionFile() {
      SOPln("\nWhat is the name of this assignment/quiz/test/etc ?\n");
      String fileName = scanner.nextLine();
      if( !fileName.toLowerCase().contains("solution") ) fileName = "Solutions_" + fileName;
      if( !fileName.contains(".txt") ) fileName += ".txt";
      if( !createTextFile( fileName ) ) return; //Create new text file with name. If something goes wrong, return
      
      SOPln("\nWhat numbers are part of the assignment?\n\tEg. Enter individual numbers, like 1,2,5,10,12,13,15,\n" +
            "\tor use ranges, such as 1,3,5,10-15,17,30-32, or you may specify odd or evens,\n" +
            "\tsuch as 1-20 evens, or a combination of these:\n" +
            "\t1,4,5,8-20 evens,23,26,27,29,41-45 odds. You can also specify subsections,\n" +
            "\tsuch as the following: 1,4,5a-d,7,8-10,14-18 evens,20b,21c,23c-g,25-31 odds,33-35");
      String problems = scanner.nextLine();
      String[] problemList = createProblemList( problems );
      String problemStringImmutabilityIssuesSoUseThis = toSingleString( problemList );
      if( problemList.length == 0 ) return;
      
      String[] numbersAndSolutions = addSolutions( problemList );
      
      //Create solution file and template file for students
      writeToFile( fileName, toSingleString( numbersAndSolutions ) );
      String templateFileName = "Temp_" + fileName.replaceAll("Solutions_","");
      writeToFile( templateFileName, problemStringImmutabilityIssuesSoUseThis );
      
      //Add macro substitutions
      addMacroSubstitutions( fileName );
      
      SOPln("\nSuccessfully created \"" + fileName + "\"");
      SOPln("\nSuccessfully created \"" + templateFileName + "\"");
   }
   
   /**
      Reformats solution files to add alternate solutions
   */
   public static void reformatSolutionFile() {
      File solutionFile = getSolutionFile();
      
      addMacroSubstitutions( solutionFile.getName() );
   }
   
   /**
      Adds any substitutions to a solution file.
      
      These substitutions, or macros, are for the automatic expansion of
      common alternative answers. This method also breaks apart lists of
      alternate answers that share the same specs by breaking these into
      separate parts, as designated by the "|" punctuation.
      
      Algebraic substitutions are included here, a topic of which can be
      continually improved upon
      
      @param fileName The solution file to change
   */
   private static void addMacroSubstitutions( String fileName ) {
      Scanner sc = getScanner( fileName );
      if( sc == null ) return;
      
      //Go through solution file
      String newLine = "";
      while( sc.hasNextLine() ) {
         String line = sc.nextLine().toLowerCase();
         String[] parts = line.split("&");
         int singleSpecBuffer = 0;
         //Go through parts (alternate solutions)
         for( int partNum = 0; partNum < parts.length; partNum++ ) {
            String[] pieces = parts[ partNum ].split(";");
            String solution = pieces[0];
            String pieceLine = "";
            //Get piece information
            for( int pieceNum = 1; pieceNum < pieces.length; pieceNum++ ) {
               pieceLine += ";" + pieces[ pieceNum ];
            }
            //I wrote this in the airport having not slept for like 48 hours. I don't even know what's going on
            if( pieces.length > 0 && pieces.length != 1 && partNum == parts.length - 1 )
               pieceLine += " "; //Need additional spaces for alternate solutions using pipes
            else if( pieces.length > 0 && pieces.length == 1 && partNum == parts.length - 1 )
               singleSpecBuffer = 1;
            
            if( solution.contains("|") ) {
               String[] alts = solution.split("\\|");
               //Go through alternates with the same piece information
               for( int altNum = 0; altNum < alts.length; altNum++ ) {
                  //Apply algebraic solutions, if setting is turned on
                  ArrayList<String> substitutions = null;
                  if( includeAlgebraicAlternates )
                     substitutions = applyAlgebraicMacros( alts[ altNum ] );
                     
                  if( substitutions != null ) {
                     int totalSubs = substitutions.size();
                     for( int rep = 0; rep < totalSubs; rep++ ) {
                        newLine += substitutions.get( rep ) + pieceLine + "&";
                     }
                  } else //No algebraic substitutions necessary
                     newLine += alts[ altNum ] + pieceLine + "&";
               }
            //No lists of alternate solutions with the same piece information
            } else {
               //Apply algebraic solutions, if setting is turned on
               ArrayList<String> substitutions = null;
               if( includeAlgebraicAlternates )
                  substitutions = applyAlgebraicMacros( solution );
                  
               if( substitutions != null ) {
                  int totalSubs = substitutions.size();
                  for( int rep = 0; rep < totalSubs; rep++ ) {
                     newLine += substitutions.get( rep ) + pieceLine + "&";
                  }
               } else //No algebraic substitutions necessary
                  newLine += solution + pieceLine + "&";
            }
         } //end parts loop
         newLine = newLine.substring( 0, newLine.length() - 2 + singleSpecBuffer ); //remove last space and &
         newLine += "\n";
      } //end while loop for lines of text file
      newLine = newLine.substring( 0, newLine.length() - 1 ); //remove last \n
      
      writeToFile( fileName, newLine );
   }
   
   /**
      Apply a series of algebraic substitutions to the given solution.
      
      These algebraic substitutions are appended to the list of given solutions
      or alternate solutions, or partial credit solutions, along with all of the
      attached specs to that solution, with the idea being that these alternate
      solutions are equivalent in form
      
      Here are some examples:
      
      1. Unary Algebraic Equivalence
         a. 1x = x
         b. x^1 = x
      2. Decimal Floating Zero Equivalence
         a. 8.0 = 8
         b. 014 = 14
      3. Associative Property Equivalence
         a. xy = yx
         b. x*y = xy
         c. x*y = y*x
      4. Communicative Property Equivalence
         a. x + y = y + x
         b. x - y = - y + x
      5. Parenthetical Equivalence
         a. x(y) = x * y
         b. x^y = x^(y)
         c. x/yz = x/(yz)
         d. -(x + y) = - x - y
         e. 5,4 = (5,4)
      
      @param solution The solution to make substitutions on
      @return ArrayList<String> The list of substitutions to make on the solution
   */
   private static ArrayList<String> applyAlgebraicMacros( String solution ) {
      ArrayList<String> substitutions = new ArrayList<String>();
      substitutions.add( solution );
      
      
      //Unary Algebraic Equivalence Substitutions

      
      //Decimal Floating Point Value Equivalence Subs
      
      
      //Associative Property Equivalence Subs
      
      
      //Communicative Property Equivalence Subs
      
      
      //Parenthetical Equivalence Subs
      
      
      return substitutions;
   }
   
   /**
      Separates a list of problems into their individual String pieces
      
      Handles ranges separated by -'s, determining odds or evens from ranges, and allowing
      the specification of subsections or parts, such as 6a, or 6a-d
      
      Creates the solution file and the corresponding student response template text files in the
      current directory

      @param problems A line of problems to interpret into its constituents. These should be delimited by
                      commas, and use hyphens for ranges, specify odds or evens for those ranges (an option),
                      and allow for subsections of problems
      @return String[] The list of problems, separated from all ranges, and interpreted from being odd or even,
                       if applicable
      
      @see createSolutionFile()
      @see createStudentResponseTemplate()
   */
   public static String[] createProblemList( String problems ) {
      String[] numberSets = problems.split(",");
      ArrayList<String> numbers = new ArrayList<String>();
      
      for( int i = 0; i < numberSets.length; i++ ) {
         numberSets[i] = numberSets[i].toLowerCase().replaceAll("\\s+","");
         
         //Single number
         int length = numberSets[i].length();
         if(      length == 0 ) continue;
         else if( length == 1 && isNumeric( numberSets[i] ) ) {
            numbers.add( numberSets[i] + ". " );
            continue;
         } else if( !numberSets[i].contains("-") ) {
            numbers.add( numberSets[i] + ". " );
            continue;
         }
         
         //Range
         String[] minToMax = numberSets[i].split("-");
         if( minToMax.length != 2 ) {
            SOPln("\nPlease only use one range per set of commas.\nPlease try again.");
            pause();
            return new String[]{};
         }
         //Determine type of range
         boolean hasParts = true;
         boolean usesEvensOrOdds = true;
         if( isNumeric( minToMax[0] ) ) hasParts = false;
         if( (!hasParts && isNumeric( minToMax[1] )) || 
             (hasParts && !isNumeric( minToMax[1] )) ) usesEvensOrOdds = false;
         //Get total problems in range
         int min, max, range;
         min = max = range = 0;
         int letterMin = 0;
         String[] minSplit = new String[0];
         if( !hasParts && !usesEvensOrOdds ) {
            try {
               min = Integer.parseInt( minToMax[0] );
               max = Integer.parseInt( minToMax[1] );
            } catch( NumberFormatException e ) {
               e.printStackTrace();
            }
            
            range = max - min + 1;
            
         } else if( !hasParts && usesEvensOrOdds ) {
            try {
               min = Integer.parseInt( minToMax[0] );
               max = Integer.parseInt( minToMax[1].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")[0] );
            } catch( NumberFormatException e ) {
               e.printStackTrace();
            }
            if( numberSets[i].contains("even") ) {
               if( min % 2 != 0 ) ++min;
               if( max % 2 != 0 ) --max;
            } else {
               if( min % 2 == 0 ) ++min;
               if( max % 2 == 0 ) --max;
            }
            
            range = ((max - min)/2) + 1;
            
         } else if( hasParts ) {
            minSplit = minToMax[0].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            String[] maxSplit = minToMax[1].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            try {
               //Range looks like 23a-23g
               if( maxSplit.length != 1 ) {
                  min = Integer.parseInt( minSplit[0] );
                  max = Integer.parseInt( maxSplit[0] );
               }
               //Range looks like 23a-g
               else {
                  min = Integer.parseInt( minSplit[0] );
                  letterMin = minSplit[1].charAt(0);
                  max = maxSplit[0].charAt(0);
               }
            } catch( NumberFormatException e ) {
               e.printStackTrace();
            }
            
            if( letterMin == 0 ) range = max - min + 1;
            else                 range = max - letterMin + 1;

         } else {
            SOPln("\nAn error has occurred.\nDid you try to use a range with a number that has parts or subsections\n" +
                  "while also specifying odds or evens?\nIf so, please try again without using a range with parts of subsections,\n" +
                  "since the program does not account for this.");
            return new String[]{};
         }
         //Add problems to the list
       
         for( int j = 0, k = 0; k < range; k++ ) {
            //Get correct number
            String token = "";
            if( !hasParts ) token = (min + j++) + "";
            else            token = min + "";
            
            //Get correct letter (or no letter)
            if( hasParts ) token += (char)(letterMin++) + ". ";
            else           token += ". ";
            
            numbers.add( token );
            
            if( usesEvensOrOdds ) j++;
         }
      }
      
      return numbers.toArray( new String[ numbers.size() ] );
   }
   
   /**
      Add the solutions to the list of numbers. These solutions are taken from the user
      one line at a time or one alternate answer at a time, depending on the setting
      'manuallyEnterSolutions'
      
      Solutions can include multiple correct or partial answers by separating answers using
      the & symbol as a delimiter. Furthermore, alternate solutions can have addition tags
      using any of the following formats:
      
      ALT_SOLUTION ; 0.5       --> Gives 1/2 credit for the given alternate solution. Any decimal works
      ALT_SOLUTION ; range 2   --> Allows for any answers within 2 of the alternate solution to be accepted
                                   as a correct answer or a partially correct answer (which is denoted by a
                                   tag that is a decimal below 1
      ALT_SOLUTION ; range 2-5 --> Only accepts answers within 5 of the answer, but not within 2 of the answer.
                                   Perhaps odd sounding, you could use this tag for giving partial credit within
                                   a range of the answer, while giving full credit for an answer using the
                                   exact answer, or an answer within a given range. Any ranges that overlap would
                                   default to this tag rather than the single range tag
      ALT_SOLUTION ; AUTO_FDBK --> AUTO_FDBK could be any length sentence that would provide feedback to the
                                   student regarding this answer. Useful for suggesting possible mistakes based
                                   on the value of the answer
      
      These tags can all be used, or some may be used, or just one, or none. An example below of all three types
      of tags being used:
      
      50 ; range 1 ; 0.5 ; May have forgot to divide both sides of the equation by 2
      
      --> This shows a partial credit answer which would be triggered for any answer between 49-51. The feedback
          provided suggests a possible algebraic error for how they may have gotten this answer, but not the
          full credit answer
      
      Full example, including a given problem (which would not be written in any of these text files) and the
      multiple alternate answers allowed for full credit, partial credit answers,
      and auto-feedback explanations for partial credit answers
      
      Question 16a: Solve the following equation by factoring: 19x = 7 - 6x^2
      
      16a. 1/3, -7/2 & -7/2, 1/3 & 0.33, -3.5 & -3.5, 0.33 & -1/3, 7/2; 0.5 ; Swap minus sign & 7/2, -1/3;0.5;Swap minus sign & -0.33, 3.5;0.5;Swap minus sign & 3.5,-1/3;0.5;Swap minus sign
      
      --> Note that lists can be tedious. Turn off the setting 'listOrderMatters' to not have to account
          for order. This requires students to separate answers using a comma as a delimiter
      --> Note also that accounting for both fractions and decimals can be tedious. Turn on the setting
          'convertFractionsToDecimals' to check answers in their fractional or decimal form automatically
      
      Using both settings 'listOrderMatters' and 'convertFractionsToDecimals' allows the line above to be
      reduced to the following:
      
      16a. 1/3, -7/2 & -1/3, 7/2;0.5;Swap minus sign
      
      Not too bad.
      
      @param problemList The list of problems numbers to print in order for the user to know which problem
                         they are entering a solution for
      @return String[] The list of solution lines, including the numbers from the problemList
      
      @see addSolutionsManually( String[] problemList )
      @see addSolutionsQuickly( String[] problemList )
      
      @see manuallyEnterSolutions See tag
      @see listOrderMatters See tag
      @see convertFractionsToDecimals See tag
   */
   public static String[] addSolutions( String[] problemList ) {
      if( manuallyEnterSolutions ) {
         SOPln("\nThe program will prompt you to enter solutions, alternature solutions,\n" +
               "partial credit ratios (as decimals), accepted ranges from the given solution\n" +
               "(which should be written in the format 'range #', where # is the range from the\n" +
               "solution that answers will be accepted), and any automatic feedback that you\n" +
               "want to give for that particular solution, alternate solution, or incorrect\n" +
               "solution that will be receiving partial credit. Once you get the hang of\n" +
               "entering solutions like this, you can turn off this tutorial-like setting\n" +
               "and enter solutions manually by changing the grading options in the menu.");
         return addSolutionsManually( problemList );
      } else {
         SOPln("Enter solutions for each number. Use & to separate solutions. Use ; for tags.\n" +
               "\tExample: 50 ; Correct good job! & 25 ; 0.5 ; You may have divided by 2 accidentally & 100/2;Correct!");
         return addSolutionsQuickly( problemList );
      }
   }
   
   /**
      Add solutions manually by prompting the user for each part of each solution
      
      The user does not need to worry about the correct syntax and symbols used by
      using this method
      
      @param problemList The list of problem numbers, whose solutions will be added following each one
      @return String[] The list problem numbers and the list of solutions / alternate solutions and tags
      
      @see addSolutions( String[] problemList )
      @see addSolutionsQuickly( String[] problemList )
   */
   private static String[] addSolutionsManually( String[] problemList ) {
      for( int i = 0; i < problemList.length; i++ ) {
         boolean enterAnotherSolution;
         String number = problemList[i];
         do {
            enterAnotherSolution = false;
         
            //Solution
            SOPln("\nEnter a solution for #" + number.split("\\.")[0] + ":");
            SOP( number );
            String solution = scanner.nextLine();
            solution = checkAndConvertToDecimal( solution );
            problemList[i] += solution;
            
            //Partial Credit
            SOPln("Is this worth less than 100% credit? (Enter yes or no)");
            String response = scanner.nextLine();
            if( response.toLowerCase().contains("y") ) {
               SOPln("How much partial credit? (Enter a decimal or a percentage)");
               response = scanner.nextLine();
               if( !isNumeric( response ) )
                  response = response.replaceAll("%","");
               if( response.contains(".") ) {
                  try {
                     double value = Double.parseDouble( response );
                     if( value >= 1.0 )
                        problemList[i] += ";" + (value/100.0);
                     else
                        problemList[i] += ";" + value;
                  } catch( NumberFormatException e ) {
                     e.printStackTrace();
                  }
               } else {
                  try {
                     int value = Integer.parseInt( response );
                     problemList[i] += ";" + (((double)value)/100.0);
                  } catch( NumberFormatException e ) {
                     e.printStackTrace();
                  }
               }
            }
            
            //Range
            SOPln("Is there a range of acceptable answers? (Enter yes or no)");
            response = scanner.nextLine().toLowerCase();
            if( response.contains("y") ) {
               boolean failure = false;
               do {
                  SOPln("Enter a range value. You can also set a minimum range using the format MIN_RANGE - MAX_RANGE");
                  response = scanner.nextLine().toLowerCase().replaceAll("\\s+","");
                  if( response.contains(".") || response.contains("range") ) {
                     failure = true;
                     SOPln("Incorrect format. Please enter only whole numbers. If using minimum ranges,\n" +
                           "separate using a hyphen, eg. 2-5. Otherwise, enter just a single number.");
                  }
               } while( failure );
               problemList[i] += ";range " + response;
            }
            
            //Automated Response
            SOPln("Does this answer have an automated feedback response? (Enter yes or no)");
            response = scanner.nextLine().toLowerCase();
            if( response.contains("y") ) {
               SOPln("Enter the feedback given for this answer:");
               problemList[i] += ";" + scanner.nextLine();
            }
            
            //Alternate solution? --> Run loop again
            SOPln("Enter another solution? (Enter yes or no)");
            response = scanner.nextLine().toLowerCase();
            if( response.contains("y") ) {
               enterAnotherSolution = true;
               problemList[i] += " & ";
            }
            
         } while( enterAnotherSolution );
      }
      
      return problemList;
   }
   
   /**
      If the setting 'convertFractionsToDecimals' is on (true), converts the fractions to decimals
      for the given solution. Decimal places are limited to rounding to the hundredths place
      
      @param solution The solution to check and convert
      @return String The converted solution
   */
   private static String checkAndConvertToDecimal( String solution ) {
      if( convertFractionsToDecimals && solution.contains("/") ) {
         String[] beforeAfterSlash = solution.split("/");
         if( beforeAfterSlash.length <= 2 ) {
            try {
               double numerator   = Double.parseDouble( beforeAfterSlash[0] );
               double denominator = Double.parseDouble( beforeAfterSlash[1] );
               DecimalFormat df = new DecimalFormat("#.##"); //Force max of two decimal places
               solution = df.format(numerator / denominator) + "";
            } catch( NumberFormatException e ) {
               //Do nothing
            } catch( ArithmeticException e ) {
               SOPln("\nYou tried to divide by zero!\n");
               //Do nothing
            }
         }
      }
      
      return solution;
   }
   
   /**
      Add solutions quickly, prompting the user to write the correct punctuation, such as semicolons and
      ampersands. The user has to write the entire line of solutions and alternate solutions and tags
      all within one line
      
      @param problemList The list of problem numbers, whose solutions will be added following each one
      @return String[] The list problem numbers and the list of solutions / alternate solutions and tags
      
      @see addSolutions( String[] problemList )
      @see addSolutionsManually( String[] problemList )
   */
   private static String[] addSolutionsQuickly( String[] problemList ) {
      for( int i = 0; i < problemList.length; i++ ) {
         SOP( problemList[i] );
         problemList[i] += scanner.nextLine().toLowerCase().trim();
         if( convertFractionsToDecimals ) {
            String[] solutions = problemList[i].split("&");
            for( int j = 0; j < solutions.length; j++ ) {
               solutions[j] = solutions[j].trim();
               String[] parts = solutions[j].split(";");
               String solution = "";
               if( j == 0 ) solution = parts[0].substring( parts[0].indexOf(" ") + 1 ).trim();
               else         solution = parts[0].trim();
               
               if( !solution.contains("/") ) {
                  if( j != solutions.length - 1 )
                     solutions[j] += " & ";
                  continue;
               }
               
               String tags = "";
               int semicolonIndex = solutions[j].indexOf(";");
               if( semicolonIndex != -1 ) tags = solutions[j].substring( semicolonIndex );
               solution = checkAndConvertToDecimal( solution );
               if( parts.length > 1 && semicolonIndex != -1 )
                  solutions[j] = parts[0].substring( 0, parts[0].indexOf(" ") + 1 ) + solution + tags;
               else
                  solutions[j] = parts[0].substring( 0, parts[0].indexOf(" ") + 1 ) + solution;
               if( j != solutions.length - 1 ) solutions[j] += " & ";
            }
            problemList[i] = toSingleString( solutions, false );
         }
      }
      
      return problemList;
   }
   
   /**
      Edit a file. The user can edit lines, add lines, or remove lines
      
      @see editFile( File file )
   */
   public static void editSolutionFile() {
      File solutionFile = getSolutionFile();
      SOPln();
      editFile( solutionFile );
   }
   
   /**
      Edit a file. The user can edit lines, add lines, or remove lines
      
      @param file The file to be editted
      @return ArrayList<ArrayList<String>> A list of the lists of changes to be made to all other files.
                                           If just one file is being edited, this return is not used
   */
   private static ArrayList<ArrayList<String>> editFile( File file ) {
      printFile( file );
      String response = "";
       ArrayList<ArrayList<String>> changeList = new ArrayList<ArrayList<String>>();
      
      do {
         SOPln("\nHow would you like to edit this file? (Enter 1, 2, 3, 4, 5, or Q)\n" +
               "\t1. Edit line\n" +
               "\t2. Add line\n" +
               "\t3. Remove line\n" +
               "\t4. View file\n" +
               "\t5. Reorder problem numbers\n\n" +
               "\tQ. Quit\n");
         response = scanner.nextLine().toLowerCase().trim().replaceAll("\\.","");
         if( response.contains("q") ) break;
         
         int option = 0;
         try {
            option = Integer.parseInt( response );
         } catch( NumberFormatException e ) {
            SOPln("Invalid response. Please enter a whole-number without any punctuation\n" +
                  "or 'Q' to stop editing");
         }
         
         if(      option == 1 ) changeList.add( editLineOfFile( file ) );
         else if( option == 2 ) changeList.add( addLineToFile( file ) );
         else if( option == 3 ) changeList.add( removeLineFromFile( file ) );
         else if( option == 4 ) printFile( file );
         else if( option == 5 ) changeList.add( reorderFile( file ) );
         
      } while( !response.contains("q") );

      return changeList;
   }
   
   /**
      Edit a line in a solution file, with prompts
      
      @param file The file to edit
      @return ArrayList<String> The list of edits to be made
   */
   private static ArrayList<String> editLineOfFile( File file ) {
      String[] problemNumbers = getProblemNumbers( file );
      String commaList = toCommaDelimitedList( problemNumbers );
      SOPln("Which problem would you like to edit?");
      SOPln("Choose one #: " + commaList );
      String response = scanner.nextLine().toLowerCase().trim();
      int problemIndex = indexOf( response, problemNumbers );
      SOPln("\nEnter the new solution line for #" + response + ".\nRemember to use & to separate solutions " +
            "and ; to separate tags.\n");
      String newSolution = scanner.nextLine().trim();
      
      editLineOfFile( file, response, problemNumbers, problemIndex, newSolution );
      
      ArrayList<String> editVariables = new ArrayList<String>();
      
      editVariables.add( "EDIT" );
      editVariables.add( response );
      editVariables.add( newSolution );
      
      return editVariables;
   }
   
   /**
      Edit a line in a solution file, without prompts
      
      Used in the editAllFiles(...) method
      
      @param file The file to edit
      @param problemNumber The problem number's line to edit
      @param line The line to change the text to
   */
   private static void editLineOfFile( File file, String problemNumber, String line ) {
      String[] problemNumbers = getProblemNumbers( file );
      int problemIndex = indexOf( problemNumber, problemNumbers );
      
      editLineOfFile( file, problemNumber, problemNumbers, problemIndex, line );
   }
   
   /**
      Edit a line in a solution file
      
      @param file The file to edit
      @param problemNumber The problem number's line to edit
      @param line The line to change the text to
   */
   private static void editLineOfFile( File file, String problemNumber, String[] problemNumbers, int problemIndex, String line ) {
      Scanner sc = getScanner( file );
      
      for( int i = 0; i < problemNumbers.length && sc.hasNextLine(); i++ ) {
         if( i != problemIndex )
            problemNumbers[i] = sc.nextLine();
         else {
            problemNumbers[i] = problemNumbers[i] + ". " + line;
            sc.nextLine(); //Throw away line
         }
      }
      
      sc.close();
      
      writeToFile( file.getPath(), toSingleString( problemNumbers ) );
   }
   
   /**
      Get a list of the problem numbers in a File
      
      @param file The file to get the problem numbers from
      @return String[] The list of problem numbers (does not include periods)
   */
   private static String[] getProblemNumbers( File file ) {
      ArrayList<String> problems = new ArrayList<String>();
      
      Scanner sc = getScanner( file );
      
      while( sc.hasNextLine() )
         problems.add( sc.nextLine().split("\\.")[0] );
      
      sc.close();
      
      return problems.toArray( new String[ problems.size() ] );
   }
   
   /**
      Add a line to a file
      
      @param file The file to edit
      @return ArrayList<String> The info used for adding these lines to files.
                                Used when changing multiple files only
   */
   private static ArrayList<String> addLineToFile( File file ) {
      SOPln("Problem numbers: " + toCommaDelimitedList( getProblemNumbers( file ) ) + "\n");
      SOPln("What is the number of this new problem?");
      String number = scanner.nextLine().trim().toLowerCase();
      if( !number.contains(".") ) number += ". ";
      if( !number.contains(" ") ) number += " ";
      SOPln("Enter the solutions and tags for #" + number.replaceAll("\\.","").trim() + ":\n" +
            "Separate solutions using & and separate tags using ;\n" +
            "If this is not a solution file, only enter a single solution.\n");
      String solution = scanner.nextLine().trim();
      
      addToFile( file.getPath(), "\n" + number + solution );
      
      ArrayList<String> addInfo = new ArrayList<String>();
      
      addInfo.add( "ADD" );
      addInfo.add( number );
      addInfo.add( solution );
      
      return addInfo;
   }
   
   /**
      Add a line to a file, without prompts
      
      Used in the editAllFiles(...) method
      
      @param file The file to edit
      @param number The problem number of the line
      @param line The line to add
   */
   private static void addLineToFile( File file, String number, String line ) {
      if( !number.contains(".") ) number += ". ";
      if( !number.contains(" ") ) number += " ";
      
      addToFile( file.getPath(), "\n" + number + line );
   }
   
   /**
      Remove a line from a file, with prompts
      
      @param file The file to edit
      @return ArrayList<String> The info used for removing lines. Only used if
                                removing lines from multiple files
   */
   private static ArrayList<String> removeLineFromFile( File file ) {
      String[] numberList = getProblemNumbers( file );
      SOPln("Problem numbers: " + toCommaDelimitedList( numberList ) + "\n");
      SOPln("Which number do you want to remove?");
      String number = scanner.nextLine().toLowerCase().replaceAll("\\.","").trim();
      
      removeLineFromFile( file, numberList, number );
      
      ArrayList<String> removeInfo = new ArrayList<String>();
      
      removeInfo.add( "REMOVE" );
      removeInfo.add( number );
      
      return removeInfo;
   }
   
   /**
      Remove a line from a file, without prompts
      
      Used in the editAllFiles(...) method
      
      @param file The file to edit
      @param problemNumber The problem number of this line
   */
   private static void removeLineFromFile( File file, String problemNumber ) {
      String[] numberList = getProblemNumbers( file );
      
      removeLineFromFile( file, numberList, problemNumber );
   }
   
   /**
      Remove a line from a file
      
      @param file The file to edit
      @param numberList The list of numbers in this file
      @param problemNumber The problem number of this line
   */
   private static void removeLineFromFile( File file, String[] numberList, String problemNumber ) {
      String fileText = "";
   
      Scanner sc = getScanner( file );
      
      for( int i = 0; i < numberList.length && sc.hasNextLine(); i++ ) {
         String line = sc.nextLine();
         if( !problemNumber.equals( numberList[i] ) )
            fileText += line + "\n";
      }
      
      sc.close();
      
      writeToFile( file.getPath(), fileText.substring( 0, fileText.length() - 1 ) );
   }
   
   /**
      Create an empty template file from an existing solution file.
      The template file will have all the problem numbers, but no answers
   */
   public static void createStudentResponseTemplate() {
      File solutionFile = getSolutionFile();
      if( solutionFile == null ) return;
      String[] problemNumbers = getProblemNumbers( solutionFile );
      appendEachToken( problemNumbers, ". " );
      String templateFileName = "Temp_" + solutionFile.getName().replaceAll( "Solutions_", "" );
      if( !createTextFile( templateFileName ) ) return;
      
      writeToFile( templateFileName, toSingleString( problemNumbers ) );
   }
   
   /**
      Check for plagiarism by comparing incorrect answer patterns across multiple HW assignments
      
      Plagiarism is determined by the following algorithm:
      
         Likelihood of Plagiarism = for all assignments: sum(#s wrong but shared / total #s) / total assignments
      
      If the percent average sum of shared wrong answers across assignments is greater than 50% for any two students,
      for any given combination, then those two students will be considered to have been plagiarizing. If the students
      sit next to each other, the percentage for comparison is 30%
   */
   private static void checkForPlagiarism() {
      File[] fileList = getTextFiles( new String[]{"Temp", "Settings", "Solutions"} );
      
      
   }
   
   /**
      Generates statistics for a set of graded files
   */
   private static void generateStatistics() {
      File[] fileList = getTextFiles( new String[]{"Temp", "Settings", "Solutions"} );
      
      SOPln("\nWhich files do you want statistics for?\nEnter the subset name identifier, eg. 'HW1'" + 
            " for one assignment, or enter 'HW' for all homework files\nmore egs. 'Quiz1', 'Test', etc");
      String searchToken = scanner.nextLine() + "_Grade";
      boolean gradeCategory = false;
      if( !hasNumber( searchToken ) )
         gradeCategory = true;
      
      double cumulativeGrade = 0.0, totalGrade = 0.0;
      ArrayList<Double> cumulativeGrades = new ArrayList<Double>();
      ArrayList<Double> totalGrades      = new ArrayList<Double>();
      int totalFileCount = 0;
      
      DecimalFormat df = new DecimalFormat("#.##"); //Force max of two decimal places
      
      for( File file: fileList ) {
         if( file.getName().toLowerCase().contains( searchToken.trim().toLowerCase() ) ) {
            Scanner sc = getScanner( file );
            sc.nextLine(); //move past name
            int lineNumber = 0; //technically, line 1, but we need to start at 0 for the list
            while( sc.hasNextLine() ) {
               String line = sc.nextLine();
               
               //Set cumulatives
               if( !gradeCategory && !line.equals("") && line.contains("Correct") ) {
                  if( totalFileCount == 0 )
                     cumulativeGrades.add( 1.0 );
                  else
                     cumulativeGrades.set( lineNumber, cumulativeGrades.get( lineNumber ) + 1.0 );
               } else if( !gradeCategory && line.equals("") ) {
                  lineNumber++;
                  continue;
               } else if( !sc.hasNextLine() ) {
                  String[] tokens = line.split(" ");
                  cumulativeGrade += Double.parseDouble( tokens[1] );
                  totalGrade += Double.parseDouble( tokens[3] );
                  break;
               } else {
                  //Incorrect answer
                  if( totalFileCount == 0 )
                     cumulativeGrades.add( 0.0 );
               }
               
               //Set totals
               if( totalFileCount == 0 )
                  totalGrades.add( 1.0 );
               else
                  totalGrades.set( lineNumber, totalGrades.get( lineNumber ) + 1.0 );
               
               lineNumber++;
            }
            
            totalFileCount++;
         }
      }
      
      //Find stats per question
      int totalQuestions = cumulativeGrades.size();
      if( !gradeCategory )
      for( int rep = 0; rep < totalQuestions; rep++ ) {
         double qCumulative = cumulativeGrades.get(rep) / defaultPointValue;
         double qTotal      = totalGrades.get(rep) / defaultPointValue;
         SOPln("Question " + (rep+1) + ": " + (int)qCumulative + " / " + (int)qTotal + ", " + df.format((qCumulative / qTotal)*100.0) + "%");
      }
      
      //Find total stats
      double totalCumulative = cumulativeGrade / totalFileCount;
      double totalTotal      = totalGrade / totalFileCount;
      SOPln("\nTotal: " + df.format(totalCumulative) + " / " + totalTotal + ", " + df.format((totalCumulative / totalTotal)*100.0) + "%");
      
   }
   
   /**
      Edit all text files at the same time
      All changes made to one student file will be "simultaneously"
      applied to all other student files without requiring
      additional prompts.
      
      This method is useful for editing all student files at the same time.
      
      Suppose you chose to not grade one or more problems on the HW. Just use
      this method to remove each problem from all students' HW. Similarly, you can
      edit all student submissions at once, or add lines to all student submissions
      
      @see editAllFiles( String inclusionToken )
   */
   public static void editAllFiles() {
      SOPln("\nWhat group of files do you want to edit?\n" +
            "(Enter the identifying name of this group, such as\n" +
            "\"HW6\" or \"Quiz2\"\n");
      String inclusionToken = scanner.nextLine();
      
      editAllFiles( inclusionToken );
   }
   
   /**
      See comments on editAllFiles()
      
      @param inclusionToken The token that all files should have in its name in order
                            to be edited
   */
   private static void editAllFiles( String inclusionToken ) {
      File[] files = getTextFiles( new String[]{"Temp","Settings","Solution"} );
      ArrayList<ArrayList<String>> changeList = editFile( files[0] );
      String[] identifiers = {"EDIT", "ADD", "REMOVE", "REORDER"};
      int changeListSize = changeList.size();
      
      int totalFiles = files.length;
      if( totalFiles == 1 ) return;
      
      for( int rep = 1; rep < totalFiles; rep++ ) {
         for( int changeIndex = 0; changeIndex < changeListSize; changeIndex++ ) {
            File file = files[rep];
            if( files[rep].getName().toLowerCase().contains( removeWhitespace( inclusionToken.toLowerCase() ) ) ) {
               String changeIdentifier = changeList.get(changeIndex).get(0);
               if(        changeIdentifier.equals( identifiers[0] ) ) { //EDIT
                  editLineOfFile( file, changeList.get(changeIndex).get(1), changeList.get(changeIndex).get(2) );
               } else if( changeIdentifier.equals( identifiers[1] ) ) { //ADD
                  addLineToFile( file, changeList.get(changeIndex).get(1), changeList.get(changeIndex).get(2) );
               } else if( changeIdentifier.equals( identifiers[2] ) ) { //REMOVE
                  removeLineFromFile( file, changeList.get(changeIndex).get(1) );
               } else {                                                 //REORDER
                  reorderFile( file );
               }
            }
         }
      }
   }
   
   /**
      Edit a response file by changing, adding, or removing lines
      
      @see editFile( File file )
   */
   public static void editResponseFile() {
      File[] files = getTextFiles( new String[]{"Temp","Settings"} );
      SOPln("Which file would you like to edit?");
      for( int i = 0; i < files.length; i++ )
         SOPln( (i+1) + ". " + files[i].getName() );
      
      boolean tryAgain = false;
      int number = 0;
      do {
         tryAgain = false;
         SOP("\nEnter a number:");
         try {
            number = Integer.parseInt( scanner.nextLine() ) - 1;
         } catch( NumberFormatException e ) {
            SOPln("Error. The value entered needs to be a number only. Try again");
            tryAgain = true;
         }
      } while( tryAgain );
      
      editFile( files[ number ] );
   }
   
   /**
      Print the menu options
      
      @param pause True for pausing before printing, false otherwise
      @see pause()
   */
   private static void printMenu( boolean pause ) {
      
      if( pause ) pause();
      
      SOPln("----------- Grader.java -----------\n");
      SOPln("a. Grade all text files");
      SOPln("b. Grade one text file");
      SOPln("c. View / Change grading options");
      SOPln("d. Create new solution file");
      SOPln("e. Edit solution file");
      SOPln("f. Create student response template");
      SOPln("g. Check for plagiarism across text files");
      SOPln("h. Generate statistics on graded student response files");
      SOPln("i. Edit all response files");
      SOPln("j. Edit single response file");
      SOPln("k. View file");
      SOPln("l. Reformat solution file.");
      SOPln("m. Retrieve downloaded files.");
      
      SOPln("\nq. Quit program");
   }
   
   /**
      Set grader settings based on the GRADER_SETTINGS_FILE_NAME text file
   */
   private static void setGraderSettings() {
      Scanner sc = getScanner( new File( GRADER_SETTINGS_FILE_NAME ) );
      
      int settingsCounter = 0;
      while( sc.hasNextLine() ) {
         String token = sc.nextLine().split("\\?")[1].trim();
         token = token.toLowerCase();
         boolean value = false;
         int num = 0;
         if( settingsCounter != 2 )
            value = Boolean.parseBoolean( token );
         else 
            num = Integer.parseInt( token );
         
         if(      settingsCounter == 0 ) spacesMatter               = value;
         else if( settingsCounter == 1 ) useAutoAltSolutions        = value;
         else if( settingsCounter == 2 ) defaultPointValue          = num;
         else if( settingsCounter == 3 ) manuallyEnterSolutions     = value;
         else if( settingsCounter == 4 ) listOrderMatters           = value;
         else if( settingsCounter == 5 ) convertFractionsToDecimals = value;
         else if( settingsCounter == 6 ) createResultsFile          = value;
         else if( settingsCounter == 7 ) includeAlgebraicAlternates = value;
         
         settingsCounter++;
      }
      
      sc.close();
   }
   
   /**
      Get all the text in a File in one large String
      
      @param file The file to get the text from
      @return String All the text in the File
   */
   private static String getTextFromFile( File file ) {
      Scanner sc = getScanner( file );
      
      String lines = "";
      while( sc.hasNextLine() ) {
         lines += sc.nextLine() + "\n";
      }
      
      sc.close();
      
      return lines.substring( 0, lines.length() - 1 );
   }
   
   /**
      Pauses the flow of output to the console by forcing the user to press enter in order to continue
   */
   private static void pause() {
      SOPln("\nPress 'enter' to continue.");
      String response = "";
      do {
         response = scanner.nextLine();
      } while( !response.equals("") );
   }
   
   /**
      Determines if a String is a number or not
      
      @param str The String to check
      @return boolean True if it is a number (an integer or a decimal), false otherwise
   */
   private static boolean isNumeric( String str ) {
      Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
   
      if( str == null )
         return false;
         
      return pattern.matcher( str ).matches();
   }

   /**
      Determine if a String has a numerical character (0-9) or not
      
      @param str The String to check
      @return boolean True if the String has a number, false otherwise
   */
   private static boolean hasNumber( String str ) {
      return str.matches(".*\\d.*");
   }

   /**
      Determines if a String is empty or consists of only whitespace
      
      @param str The String to check
      @return boolean True of the String is only whitespace or is empty, false otherwise
      @see translateUserInput( String userInput )
      @see Character.isWhitespace( char ch )
   */
   private boolean isWhitespace( String str ) {
      if( str.isEmpty() ) return true;
      
      char[] set = str.toCharArray();
      
      for( int i = 0; i < set.length; i++ )
         if( !Character.isWhitespace( set[i] ) )
            return false;
      
      return true;
   }

   /**
      Remove all characters that are not letters
      
      @param str The String to edit
      @return String The String without nonletter characters
   */
   private static String removeNonLetters( String str ) {
      return str.replaceAll("[^a-zA-Z]+", "");
   }
   
   /**
      Remove all characters that are not numbers
      
      @param str The String to edit
      @return String The String without nonnumber characters
   */
   private static String removeNonNumbers( String str ) {
      return str.replaceAll("[^0-9]+", "");
   }
   
   /**
      Remove all characters that are not letters or numbers
      
      @param str The String to edit
      @return String The String without non-alphanumeric characters
   */
   private static String removeNonAlphanumeric( String str ) {
      return str.replaceAll("[^a-zA-Z0-9]+", "");
   }
   
   /**
      Remove all characters that are not letters or numbers or whitespace
      
      @param str The String to edit
      @return String The String without nonletters, nonnumbers, or non-whitespace characters,
                     or non-ampersands
   */
   private static String removeNonAlphanumericWhitespace( String str ) {
      return str.replaceAll("[^a-zA-Z0-9\\s]+", "");
   }
   
   /**
      Remove all characters that are not letters or numbers or whitespace
      
      @param str The String to edit
      @return String The String without nonletters, nonnumbers, or non-whitespace characters,
                     or non-ampersands
   */
   private static String removeNonAlphanumericWhitespaceAndAnd( String str ) {
      return str.replaceAll("[^a-zA-Z0-9\\s&]+", "");
   }
   
   /**
      Remove all characters that are not letters or not whitespace
      
      @param str The String to edit
      @return String The String without nonletter, non-whitespace characters
   */
   private static String removeNonLettersWhitespace( String str ) {
      return str.replaceAll("[^a-zA-Z\\s]+", "");
   }
   
   /**
      Remove all characters that are not numbers or not whitespace
      
      @param str The String to edit
      @return String The String without non-numeric, non-whitespace characters
   */
   private static String removeNonNumbersWhitespace( String str ) {
      return str.replaceAll("[^0-9\\s]+", "");
   }
   
   /**
      Remove all characters that are whitespace
      
      @param str The String to edit
      @return String The String without non-numeric, non-whitespace characters
   */
   private static String removeWhitespace( String str ) {
      return str.replaceAll("[\\s]+", "");
   }

   /**
     * Capitalized the first letter of a token and return that token
     * 
     * @param token The token to capitalize the first letter of
     * @return The same token with the first letter capitalized
     */
   private static String capFirstLetter( String token ) {
      if( !Character.isLetter( token.charAt(0) ) ) return token;
        
      if( token.length() == 1 ) return token.toUpperCase();
        
      return String.valueOf( token.charAt(0) ).toUpperCase() + token.substring( 1, token.length() );
   }
   
   /**
      Finds the index of the String in the String array.
      Returns -1 if the String is not found in the array
      
      @param str The String to look for in the array
      @param list The list of Strings
      @return int The index of the String in the array, or -1 if it is not found
   */
   private static int indexOf( String str, String[] list ) {
      int index = -1;
      for( int i = 0; i < list.length; i++ )
         if( str.equals( list[i] ) )
            index = i;
      
      return index;
   }
   
   /**
      Add the given token at the end of each String in the list
      
      @param list The list of Strings
      @param token The String to add to the end of each item in the list
      @return String[] The new list
   */
   private static void appendEachToken( String[] list, String token ) {
      for( int i = 0; i < list.length; i++ )
         list[i] += token;
   }
   
   /**
      Convert a list of Strings to a single String. Each token is separated by a line break
      
      @param list The list of Strings
      @return String The lines all put together, delimited by line breaks
   */
   private static String toSingleString( String[] list, boolean addBreaks ) {
      String str = "";
      for( String part : list ) {
         if( addBreaks ) str += part + "\n";
         else            str += part;
      }
         
      if( addBreaks ) str = str.substring(0, str.length() - 1 ); //Note that /n is just one character
         
      return str;
   }
   
   /**
      Convert a list of Strings to a single String. Each token is separated by a line break
      
      @param list The list of Strings
      @return String The lines all put together, delimited by line breaks
   */
   private static String toSingleString( String[] list ) {
     return toSingleString( list, true );
   }
    
   /**
      Converts a String list to a single String delimited by commas and separated by spaces
      
      @param list The list to convert
      @return String A String separated by commas and spaces
   */
   private static String toCommaDelimitedList( String[] list ) {
      String str = "";
      for( String token : list )
         str += token + ", ";
      
      return str.substring( 0, str.length() - 2 );
   }
   
   /**
     * Gets a File based on the file name and the relative path
     * 
     * @param String filePath The path of the File to be found
     * @return File The File found from this name. If not found, throws a FileNotFoundException
     */
   private static File getFile( String filePath ) throws FileNotFoundException {
      File dir = new File(".");
      File[] filesList = dir.listFiles();
      for( File file: filesList ) {
         if( file.getName().equals( filePath ) ) return file;
      }
        
      throw new FileNotFoundException("File not found. Path of file not found: " + filePath );  
   }
   
   /**
      Prints the contents of a File. The user chooses from a list of Files that are
      in the current directory
      
      @see printFile( File file )
   */
   private static void printFile() {
      ArrayList<File> textFiles = new ArrayList<File>( Arrays.asList( getTextFiles() ) );
      int totalFiles = textFiles.size();
   
      boolean fileFound = false;
      do {
         printFileListByName( textFiles );
         SOPln("\nWhich file?");
         String response = scanner.nextLine().toLowerCase();
         
         //If enter number index
         boolean invalidIndex = false;
         if( isNumeric( response.trim().replaceAll("\\.", "" ) ) ) {
            try {
               response = response.trim().replaceAll("\\.", "" );
               int indexPos = Integer.parseInt( response ) - 1;
               printFile( textFiles.get( indexPos ) );
               return;
            } catch( NumberFormatException e ) {
               //treat as a name entered, skip exception
            } catch( IndexOutOfBoundsException e ) {
               SOPln("\nInvalid index. Please enter a number between 1 and " + totalFiles );
               invalidIndex = true;
            }
         }
         
         if( !response.contains(".txt") ) response += ".txt";
         
         //if enter the name of the file ^ v
         if( !invalidIndex )
         for( int i = 0; i < totalFiles; i++ ) {
            if( response.equals( textFiles.get(i).getName().toLowerCase() ) ) {
               printFile( textFiles.get(i) );
               return;
            }
         }
         
         if( !invalidIndex ) SOPln("\nFile not found. Try again.");
      } while( !fileFound ); //always true if reaches this point
   }
   
   /**
      Prints the contents of a File
      
      @param file The file to be printed
   */
   private static void printFile( File file ) {
      Scanner sc = getScanner( file );
      
      while( sc.hasNextLine() )
         SOPln( sc.nextLine() );
      
      sc.close();
   }
   
   /**
      Rearrange the lines of a File by placing all problem numbers in an increasing order
      
      @param file The file to reorder
      @return ArrayList<String> Used to determine whether additional files need reordering.
                                See editFile(...)
   */
   private static ArrayList<String> reorderFile( File file ) {
      String text = getTextFromFile( file );
      String[] lines = text.split("\\R");
      
      //BubbleSort
      int n = lines.length;
      for( int i = 0; i < n - 1; i++ ) {
         for( int j = 0; j < n - i - 1; j++ ) {
            int a = getIntegerProblemNumber( lines[j] );
            int b = getIntegerProblemNumber( lines[j + 1] );
            if( a > b ||
                ( a == b && getProblemNumberLetter( lines[j] ) > getProblemNumberLetter( lines[j + 1] ) ) ) {
               String temp = lines[j];
               lines[j] = lines[j + 1];
               lines[j + 1] = temp;
            }
         }
      }
      
      writeToFile( file.getPath(), toSingleString( lines ) );
      
      ArrayList<String> reorderInfo = new ArrayList<String>();
      
      reorderInfo.add("REORDER");
      
      return reorderInfo;
   }
   
   /**
      Get the integer problem number from the given line
      
      @param line The line from the file
      @return int The integer problem number
   */
   private static int getIntegerProblemNumber( String line ) {
      String numberWithExtras = line.substring( 0, line.indexOf(".") );
      String number = removeNonNumbers( numberWithExtras );
      int value = 0;
      try {
         value = Integer.parseInt( number );
      } catch( NumberFormatException e ) {
         e.printStackTrace();
      }
      
      return value;
   }
   
   /**
      Get the letter part of the problem number from the given line
      
      @param line The line to get the letter part from
      @return char The letter part of the problem number
   */
   private static char getProblemNumberLetter( String line ) {
      String numberWithExtras = line.substring( 0, line.indexOf(".") );
      String letter = removeNonLetters( numberWithExtras );
      return letter.charAt(0);
   }
   
   /**
     * Overwrite an existing File
     * 
     * @param fileLoc The location of the File to write to
     * @param text The text to write to the File
     */
   private static void writeToFile( String fileLoc, String text ) {
      FileWriter fw = null;
      try {
         fw = new FileWriter( fileLoc );
         fw.write( text );
         fw.close();
      } catch( IOException e ) {
         e.printStackTrace();
      }
   }
    
   /**
     * Add to an existing File
     * 
     * @param fileLoc The location of the File to write to
     * @param text The text to write to the File
     */
   private static void addToFile( String fileLoc, String text ) {
      FileWriter fw = null;
      try {
         fw = new FileWriter( fileLoc, true );
         fw.write( text );
         fw.close();
      } catch( IOException e ) {
         e.printStackTrace();
      }
   }

   /**
      Creates a new text file (.txt) with the given name
      
      @param fileName The name of the text file
   */
   private static boolean createTextFile( String fileName ) {
      if( !fileName.contains(".txt") ) fileName += ".txt";
   
      try {
         File file = new File( fileName );
         if( file.createNewFile() ) {
            //@@DEBUG
            //SOPln("File created: " + file.getName());
            return true;
         } else {
            SOPln("\nError! File with the name \"" + fileName + "\" already exists.\n" +
                  "Try again with a different name");
            return false;
         }
      } catch( IOException e ) {
         e.printStackTrace();
      }
      
      return false;
   }

   /**
      Move File from one location to another
   
      @param fileName The name of the File object to be moved
      @param pathLocation The path location of the File
      @param pathDestination The path destination for the File
   */
   private static void moveFile( String fileName, String pathLocation, String pathDestination ) {
      try {
         Files.move( Paths.get( pathLocation + fileName ), Paths.get( pathDestination + fileName ) );
      } catch( IOException e ) {
         e.printStackTrace();
      }
   }

   /**
      Retrieve the downloaded set of files (presumably from Schoology or another online software)
      and move each student file to the current directory
   */
   public static void retrieveDownloadedFiles() {
      final long X_MINUTES = 300000; //5 minutes = 300000 milliseconds
      ArrayList<File> fileList = new ArrayList<File>();
   
      //Get downloads directory path
      String home = System.getProperty("user.home");
      String downloadsPathName = home + "/Downloads/";
      Path downloadsPath = Paths.get( downloadsPathName );
      
      //Get files and folders in downloads folder
      File downloadsFolder = downloadsPath.toFile();
      String[] fileNames = downloadsFolder.list();
      
      //Find folders that were created within last X_MINUTES
      for( String name : fileNames ) {
         File downloadsFile = new File( downloadsPathName + name );
         BasicFileAttributes attr = null;
         try {
            attr = Files.readAttributes( downloadsFile.toPath(), BasicFileAttributes.class );
         } catch( IOException e ) {
            e.printStackTrace();
         }
         long modifiedValue = attr.lastModifiedTime().toMillis();
         long currentTimeValue = System.currentTimeMillis();
         long timeSinceModification = currentTimeValue - modifiedValue;
         
         //Needs to be folder, not file
         if( timeSinceModification < X_MINUTES && downloadsFile.isDirectory() )
            getFilesInDirectoryAndSubdirectories( downloadsFile.getPath(), fileList );
         
      }
      
      //Move all files to current directory
      for( File file : fileList )
         moveFile( file.getName(), file.getPath().substring( 0, file.getPath().lastIndexOf("\\") + 1 ), ".\\" );
      
   }

   /**
      Get a list of all files in the directory and all subdirectories
      
      @param directoryName The path of the directory being looked at
      @param files The list of files. Call this method with an empty list of Files
   */
   private static void getFilesInDirectoryAndSubdirectories( String directoryName, List<File> files ) {
      File directory = new File( directoryName );
   
      //Get all files from a directory.
      File[] fList = directory.listFiles();
      if( fList != null ) {
         for( File file : fList ) {      
            if( file.isFile() )
               files.add( file );
            else if( file.isDirectory() )
               getFilesInDirectoryAndSubdirectories( file.getAbsolutePath(), files );
         }
      }
   }

   /** Get a Scanner object that is reading a File based on a File name */
   private static Scanner getScanner( String fileName )
   { return getScanner( new File( fileName ) ); }
   /**
      Get a Scanner object that is reading a File
      
      @param file The file to scan
      @return Scanner The scanner object that is reading the given file
   */
   private static Scanner getScanner( File file ) {
      Scanner sc = null;
      try {
         sc = new Scanner( file );
      } catch( FileNotFoundException e ) {
         e.printStackTrace();
      }
      
      return sc;
   }

   private static void SOP( String str ) {
      System.out.print( str );
   }
   
   private static void SOPln() {
      System.out.println();
   }
   
   private static void SOPln( String str ) {
      System.out.println( str );
   }

}