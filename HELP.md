# Getting Started

## Eclipse CDO

- provides means to store/load bigraphical models to/from a CDO server

Hint: net4j version of CDO Server must match with the maven dependency.
Otherwise an exception is thrown ala 
`org.eclipse.net4j.channel.ChannelException: Failed to register channel with peer: Protocol version 37 does not match expected version 34`

- Installation: Possible via the Eclipse Installer (Select Eclipse CDO Server in the list and select the correct product version, see below)

### Compatible Versions

These versions of the Eclipse CDO Server and Maven Dependencies are working together:
- emf.cdo.version + net4j + hibernate 4.5.0 with CDO Server Eclipse Neon
- emf.cdo.version + net4j + hibernate 4.7.0 with CDO Server Eclipse Oxygen

- Approaches on how to implement support for spring data repositories: https://stackoverflow.com/questions/20161437/custom-spring-data-repository-backend
    - but takes to much time to implement correctly

FAQ: https://wiki.eclipse.org/FAQ_for_CDO_and_Net4j
    - How can I react to remote changes to my objects?
    - What happens in case of a conflict (two transactions changing the same object)?
        -  transaction.options.addConflictResolver
    
### CDO Server

- Architecture of a Repository: http://download.eclipse.org/modeling/emf/cdo/drops/I20190517-0100/help/org.eclipse.emf.cdo.doc/html/programmers/server/Architecture.html

- Deploy and Start a CDO Server: https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.emf.cdo.doc%2Fhtml%2Foperators%2FDoc00_OperatingServer.html
    - start eclipse from ~/eclipse/cdo-server-neon/eclipse/ or /home/dominik/eclipse/cdo-server-oxygen/eclipse/

- Or with the eclipse cdo-server workbench. But does not work because of derby error
    - derby installation: https://medium.com/ctrl-alt-kaveet/tutorial-installing-apache-derby-4cbf03c4aaba

#### Performance Issue 

- Things to consider when evaluating the performance
    - https://www.eclipse.org/forums/index.php/t/1074305/

Tweaking:
- https://wiki.eclipse.org/CDO/Tweaking_Performance

### OCL
pure:
https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.ocl.doc%2Fhelp%2FCustomizingtheEnvironment.html

CDO-OCL Queries: we need the eclass of the entity in question
(working examples)
createQuery(cdoTransaction, "Book.allInstances()->size()", ((BookstoreDomainModelPackage)context).getBook()).getResult()
createQuery(cdoTransaction, "Book.allInstances()->size()", ((BookstoreDomainModelPackage)EPackage.Registry.INSTANCE.getEPackage(persistentEntity.getNsUri())).getBook()).getResult()
CDOQuery query = createQuery(cdoTransaction, "EObject.allInstances()", EcorePackage.eINSTANCE.getEObject());
CDOQuery query2 = createQuery(cdoTransaction, "EObject.allInstances()->size()", EcorePackage.eINSTANCE.getEObject());
cdoTransaction.createQuery("ocl", "bookstoreDomainModel::Book.allInstances()->size()").getResultValue() // not supported exception

### CDO Client

Client examples:
- https://wiki.eclipse.org/CDO/Client
- http://git.eclipse.org/c/cdo/cdo.git/tree/plugins/org.eclipse.emf.cdo.examples/src/org/eclipse/emf/cdo/examples/server/Server.java
- http://git.eclipse.org/c/cdo/cdo.git/tree/plugins/org.eclipse.emf.cdo.examples/src/org/eclipse/emf/cdo/examples/StandaloneContainerExample.java


http://www.rcp-vision.com/cdo-connected-data-objects/
https://wiki.eclipse.org/CDO/Hibernate_Store/Tutorial

### CDO Queries

- OCL, SQL, or CDO-like

https://wiki.eclipse.org/CDOQuery_OCL
https://www.eclipse.org/forums/index.php/t/760318/
https://bugs.eclipse.org/bugs/show_bug.cgi?id=435807

### CDOID

from https://www.eclipse.org/forums/index.php/t/450110/:
> So now we should have 2 versions of this Company object in the database.
> Say we make a couple of more updates to this Company record's city (2 more would make 4 versions). The CDOID will be 
> the same and uniquely identifiers it (please let me know if this is not the case. I'm fairly certain I read that a 
> CDOID is unique for a repository ("/repo1" in this case)).
Totally correct. The CDOID is unique for a CDOObject in an IRepository. All CDORevisions (in both time and branch
dimensions) of this CDOObject share the same CDOID.

Fetch by CDOID: https://www.eclipse.org/forums/index.php/t/135470/


This makes also the CDOID up:
```
String uriFragment = resource.getURIFragment(eObject);
String uriFragment = EcoreUtil.getURI(eObject).fragment();

CDOUtil.getCDOObject(eObject).cdoID();
```

Both can be used to retrieve an object from a resource.


[https://www.eclipse.org/forums/index.php/t/136782/](https://www.eclipse.org/forums/index.php/t/136782/):
> 2) detach/reattach an object
> Whenever an object is detached from the graph it looses all its
> CDO-specific properties: id, state, view and revision.


### Converters:

- For none EObject classes we need to store some additional properties. 
- One cannot add new attributes dynamically to an existing instance
- temp. extending meta-model?? is to cumbersome

- Not many suitable ways exists:
    - "No, there's no such thing as a custom property. What's described in the Ecore model is all there is available. 
    Of course you can add adapters to eAdapters() and those could carry additional data. Or you can
    maintain such data externally via a map. Or maybe http://eclipse.org/facet/ helps." 
    https://www.eclipse.org/forums/index.php/m/1595102/?srch=emf+facet#msg_1595102
    
- facets maybe suitable 
    - (see Java extension for JPA example: https://www.slideshare.net/fmadiot/emf-facet-eclipsecon-2011-audition-6175334)
    - but cannot serialized? https://www.eclipse.org/forums/index.php/m/1233819/?srch=emf+facet#msg_1233819
        - only dynamic re-computation
    - Doc: https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.emf.facet.efacet.doc%2Fmediawiki%2Fuser.html



### Conflicts

from: https://www.eclipse.org/forums/index.php/t/452339/

- just provide user-defined handlers... if not set throw an exception.

### Further Context

#### Transactions and Views

- Transaction are like parallel shared subsession over a unique distinct session
    only after commit things gets merged
    one can lock objects (w/o committing) and the other transaction gets aware of it automatically
    
- from: https://www.eclipse.org/forums/index.php/t/450110/
    A CDOView or CDOTransaction provides *consistent* access to the CDOObject *graph* at a certain point in time in a
    certain branch (e.g. the MAIN branch). You can navigate this graph through EMF API and will never leave this
    time/branch. A CDOTransaction always "looks" at the latest time in its branch, while a pure CDOView can "wind back
    time". Views and transactions are *revision selectors* , where a revision is the state of a *single* object for a
    certain period of time in a certain branch.
    
    With a transaction you can modify the object graph atomically. The transitions between two consecutive commits are
    described by CDOCommitInfos. They're returned from transaction.commit() and can later be recreated, e.g., through
    session.getCommitInfoManager() methods. It's often more convenient to use the newer CDOCommitHistory or CDOObjectHistory
    APIs as returned by implementations of CDOCommitHistory.Provider. A commit history is an ordered collection of
    CDOCommitInfos (also used by our CDOHistoryPage contribution to the Eclipse "History" view part). The availablity of
    historical commit infos and the cost to recreate them may depend on the used IStore.
    
    CDOCommitInfos contain/provide the *deltas* between two consecutive sets of CDORevisions (the *states* of relevant
    objects before and after a commit). They know nothing about CDOViews, CDOTransactions or CDOObjects (other than their
    CDOIDs). But you can always turn a CDOID into a CDOObject (and the associated object graph) by opening a CDOView at the
    time and branch that's associated with the CDOCommitInfo (and all its deltas and resulting revisions). That's what we do
    in our EMF Compare integration as you can see by double clicking a commit info in the CDOHistoryPage.

#### Revisions

from: https://www.eclipse.org/forums/index.php/t/265043/
There's a low-level API to access arbitrary revisions: CDORevisionManager, i.e. session.getRevisionManager(). But on the
CDORevision layer navigation is not directly possible.

If you want to access the entire object graph at a different, historical time it's more convenient to open a CDOView on
the same session and set the (target) timestamp. You can do that when opening the view or even later via setTimeStamp().
A CDOView has a getObject(T) method that you can use to directly jump to an object from another view (your transaction
in this case).

CDOView: "Think of a CDOView as a read-only transaction, it has most of the
         (same) methods for accessing models. In fact a CDOTransaction is a
         sub type of a CDOView"
         
         
"For a given *CDOObject* (and with auduting enabled in the server) you can
access any *revision* with these methods:

CDOUtil.getRevisionByVersion(CDOObject, int)
CDOUtil.getRevisionByVersion(CDOObject, CDOBranch, int)

If you need the "CDOObjects" for these *revisions* you must open audit views for the time stamps of these revisions:

int version = 1;
CDOView audit = object.cdoView().getSession.openView(CDOUtil.getRevision(object, version).getTimeStamp());
CDOObject oldObject = audit.getObject(object);"


Revision Count:
"You can ask for the version of the latest revision of an object by

a) If you have a view/transaction open: view.getObject(id).cdoRevision().getVersion() or

b) by using the revision manager: session.getRevisionManager().getRevision(id, mainBranch.getHead(), ...).getVersion()

If you only use one branch (the main branch) that version is equal to the number of revisions of that object."

## Spring

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.0.BUILD-SNAPSHOT/maven-plugin/)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/{bootVersion}/reference/htmlsingle/#configuration-metadata-annotation-processor)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/{bootVersion}/reference/htmlsingle/#using-boot-devtools)

