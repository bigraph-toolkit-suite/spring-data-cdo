TODO:
    [UNSURE] //TODO valid resourcepath also: cdo://repo1/sample
    -> then use the repository instead of the one in the options
    
    - add boot-starter: https://www.baeldung.com/spring-boot-custom-starter
    Simulate conflicts: create object, read with other repo, modify first one, modify second one, save both
        test (non)+ conflicting additions
    
    put all annotation based values outside template and into repo class
        ->not for everything!
    
    Check isNew function: for dobule saves, cause now we have also legacy mode support
    
    CdoTemplate: finally block: we can close the transaction if not the session 
        
    Events, Applications ?
    
    Allow definition of replica sets in CdoClient, CdoClientFactoryBean, etc...
    
    [CLOSED] Test expression in annotations
    
    change listener for cdo model
        - via adapter of eobject? or is there a CDO feature?
    
    [CLOSED] CdoServerConnectionString parser and tests
    
    - Catch OptimisticLockingException: Could not lock objects within 10000 milli seconds
        -> enable user params via clientsessionoptions
        
    - catch LocalCommitConflictException
    
    Test:
        - data could not be retrieved after saving+deleting it in one transaction and finding it within another
        
     
    throw DataAccessResourceFailureException for can't connect to CDO   
    
       
        
optimistic locking exception based on version: reload the latest snapshot, merge the specific attributes and update.
        
        
Document: add bookstore lib to local-repo:
mvn install:install-file  -Dfile=src/test/resources/bookstoreModelApi.jar \
                          -DgroupId=de.tudresden.inf.st.ecore.models \
                          -DartifactId=bookstore-api \
                          -Dversion=1.0.0 \
                          -Dpackaging=jar \
                          -DgeneratePom=true \
                          -DlocalRepositoryPath=src/test/resources/my-repo