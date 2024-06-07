Feature: Do we save correctly all data ?
  Everybody wants to know if we save them correctly

  Scenario Outline: Do we create CSV data file with the right structure
    Given Step 0 : We have some survey in directory "<Directory>"
    When Step 1 : We launch main service
    Then Step 2 : We check root output file has <ExpectedLineCount> lines and <ExpectedDataFieldCount> variables

    Examples:
    # Parameters :
    # - Directory : Directory of test campaigns
    # - ExpectedLineCount : Expected row quantity (header included)
    # - ExpectedDataFieldCount : Expected field quantity

    |Directory                        |ExpectedLineCount     |ExpectedDataFieldCount            |
    |SAMPLETEST-DATAONLY-v1           |4                     |136                               |
    |SAMPLETEST-PAPERDATA-v1          |6                     |137                               |

  Scenario Outline: Do we create Parquet data file with the right structure
    Given Step 0 : We have some survey in directory "<Directory>"
    When Step 1 : We launch main service
    Then Step 2 : We check root parquet output file has <ExpectedLineCount> lines and <ExpectedDataFieldCount> variables

    Examples:
    # Parameters :
    # - Directory : Directory of test campaigns
    # - ExpectedLineCount : Expected row quantity
    # - ExpectedDataFieldCount : Expected field quantity

      |Directory                        |ExpectedLineCount     |ExpectedDataFieldCount            |
      |SAMPLETEST-DATAONLY-v1           |3                     |136                               |
      |SAMPLETEST-PAPERDATA-v1          |5                     |137                               |