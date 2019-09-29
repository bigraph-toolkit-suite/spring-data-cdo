TODO:
    //TODO valid resourcepath also: cdo://repo1/sample
    -> then use the repository instead of the one in the options
    
    - add boot-starter: https://www.baeldung.com/spring-boot-custom-starter
    Simulate conflicts: create object, read with other repo, modify first one, modify second one, save both
        test (non)+ conflicting additions
        
    Events
    
    Allow definition of replica sets in CdoClient, CdoClientFactoryBean, etc...
    
    Test expression in annotations
    
    CdoServerConnectionString parser and tests
    
    - Catch OptimisticLockingException: Could not lock objects within 10000 milli seconds
        -> enable user params via clientsessionoptions
        
    - catch LocalCommitConflictException
    
    Test:
        - data could not be retrieved after saving+deleting it in one transaction and finding it within another
        
     
    throw DataAccessResourceFailureException for can't connect to CDO   
    
       
        
optimistic locking exception based on version: reload the latest snapshot, merge the specific attributes and update.
        