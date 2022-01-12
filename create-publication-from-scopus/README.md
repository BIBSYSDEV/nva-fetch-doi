# CREATE PUBLICATION FROM SCOPUS


## Source:
* Based on https://bitbucket.org/unit-norge-team/sentralimport/src/develop/
* SCOPUS xsd schemas collected from https://schema.elsevier.com/dtds/document/abstracts/
  * We had to comment line 1690 in resources/schema.scopus.xocs-ani515/mathml3/mathml3-presentation.xsd (due to a double 
reference)