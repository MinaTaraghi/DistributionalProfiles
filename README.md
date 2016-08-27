# DistributionalProfiles


For Farsi to English translation


You need to add mallet libraries to your project.


needs files:

          Farsi corpus: cleanParallel.fa
          
          extract.sorted.gz extracted: extract.sorted
          
          phrase-table.gz extracted: phrase-table
          
          Farsi Dev corpus: Dev.fa
          
          Farsi Test corpus: NIST.fa
          
        
assumes corpus has 99923 lines

number of topics are set equal to 50


produces:

          pseudo documents in the mallet input file format: PDs.txt
          
          phrase pairs + pseudo doduments: ppPDs.txt
          
          new phrase table: phrase-table-topics
          
          tagged Dev corpus:  Dev.fa.topics.ixml
          
          tagged Test corpus: NIST.fa.topics.ixml
          
