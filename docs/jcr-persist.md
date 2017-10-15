# JcrPersist

`JcrPersist` is a simple no-intrusion ORM style framework for JCR/CRX. It allows you to read/write
simple plain Java POJO on the JCR using Java reflection techniques. It needs no annotations
to start with and provides multiple custom extension points for usage.

All functionality of **JcrPersist** that can be used via annotations, can also be used programmatically
using the **JcrPersist** APIs. This allows one to work with POJO objects whose source cannot be modified,
such as from 3rd party frameworks like Twitter, Instagram, Facebook SDKs and all.

Following are the primary differences from **Sling Models**:

* It is 100% non-intrusive - no need to modify your POJO objects (extend from base-class/add-annotations) to work with
* There are no build time steps to be made (no POM changes)
* Any POJO can be used to both read/write objects
* Collections (including implicit collections) are supported
* Nested and composed objects are supported as well
* All customizations can be done either by use of annotations (intrusive) or programmatically via `JcrPerist` APIs (non-intrusive).

## Table of Contents

   * [JcrPersist](#JcrPersist)
      * [Usage](#usage)
         * [Simple Example using Stocks](#simple-example-using-stocks)
      * [Transient values](#transient-values)
      * [Customization tweaks available](#customization-tweaks-available)
         * [AEMPathProperty](#aempathproperty)
         * [AEMProperty](#aemproperty)
         * [AEMType](#aemtype)
         * [AEMPropertyExclude](#aempropertyexclude)
         * [AEMImplicitCollection](#aemimplicitcollection)
      * [Extension points available](#extension-points-available)
         * [TypeInstantiator](#typeinstantiator)
         * [ValueMapReader](#valuemapreader)
      * [TODO](#todo)
      * [Developers](#developers)
      
## Usage

### Simple Example using Stocks

Consider the following Java POJO

```java
public class Stocks {

	public long lastUpdated;
	
	public List<Stock> children;

}

public class Stock {

	public String name;
	
	public double price;

}
``` 

To read an object from the JCR,

```java
Stocks stocks = JcrPersist.read("/content/mysite/stocks", Stocks.class, resourceResolver);
```

To write the same updated `Stocks` instance back to JCR:

```java
JcrPersist.persist("/content/mysite/stocks", stocks, resourceResolver);
``` 

## Transient values

`JcrPersist` by-default does not read/persist transient values. In case a transient attribute
needs to be read, it can be annotated with `@AEMProperty` annotation (see below). Similarly,
if a non-transient attribute needs to be ignored inside `JcrPerist` the `@AEMPropertyExclude` annotation
can be be used for the same. See below for examples.

```java
public class Employee {

	@AEMPath
	public String employeeID;
	
	/**
	 * This transient attribute will not be serialized by JcrPerist by default. Thus
	 * to instruct JcrPerist to include this attribute as well, we can add an @AEMProperty
	 * annotation to this attribute. This will ensure that the attribute gets serialized.
	 */
	@AEMProperty("myVisitedCount")
	public transient int visitedCount;
	
	/**
	 * This attribute is non-transient and thus to exclude this attribute
	 * from JcrPerist serialization we add the annotation of @AEMPropertyExclude.
	 */
	@AEMPropertyExclude
	public long salary;
	
}
```

The above can also be achieved using the following `JcrPerist` APIs without the need of having
to add the annotations:

```java
// include the transient variable
JcrPerist.includeField(Employee.class, "visitedCount", "myVisitedCount");

// exclude the non-transient variable
JcrPerist.excludeField(Employee.class, "salary");
```

## Customization tweaks available

* AEMPathProperty
* AEMProperty
* AEMType
* AEMPropertyExclude
* AEMImplicitCollection

### AEMPathProperty

A path in the JCR repository can act as a unique key for any given object instance
read or persisted. The path does not change, and if it does, then the object instance
must be changed too.

To declare the same, one can use the `@AEMPathProperty` annotation on a field and `JcrPerist`
will use it in case available and not specified via API call:

```java
public class Stocks {

	@AEMPathProperty
	public String path;
	
	public long lastUpdated;
	
	public List<Stock> children;
	
}
```

```java
// Once the instance is read via the call:
Stocks stocks = JcrPerist.read("/content/mysite/stocks", Stocks.class, resourceResolver);

// the value of stocks.path will point to /content/mysite/stocks
Assert.assertEquals(stocks.path, "/content/mysite/stocks");
```

And once the path is set, the `save()` and `refresh()` APIs can be called:

```java
// the following call will refresh values in the instance from the JCR
JcrPerist.refresh(stocks, resourceResolver);

// and the following call will persist the values back in JCR
JcrPerist.save(stocks, resourceResolver);
```

**Note:** The above `save()` and `refresh()` calls only work when a field is annotated
with `@AEMPathProperty` annotation and the value is not-null.

### AEMProperty

When the attribute name does not match the JCR property name, `@AEMProperty` annotation
can be used to signify the property name in JCR.

```java
public class Stocks {
	
	@AEMPathProperty
	public String path;
	
	@AEMProperty("lastRefreshed")
	public long lastUpdated;
	
	public List<Stock> children;
	
}
```

The same annotation can also be used to read declared transient variables. See above for more details.

### AEMType

The annotation indicates the `jcr:primary` type value to be used when persisting a class inside the JCR
using `JcrPerist`. For example, the instances of the following class shall be persisted as `cq:Page` type when
using `JcrPerist`.

```java
@AEMType(primaryType = "cq:Page", childType = "cq:PageContent")
public class MyCustomPage {

	// normal class attributes follow
	
}
```

Now if we perist an instance of this `MyCustomPage` class using the following, 2 nodes shall be created:

* `/content/jcrpersist/pages/somePage` of the type of `cq:Page`
* `/content/jcrpersist/pages/somePage/jcr:content` of the type of `cq:PageContent`

```java
JcrPerist.persist("/content/jcrpersist/pages/somePage", myCustomPageInstance, resourceResolver);
```

### AEMPropertyExclude

This annotation is used over a class attribute to indicate that `JcrPerist` should skip the field from all
serialization/deserialization methods. This ensures that a non-transient attribute can skip persistence.
See above for more details.

### AEMImplicitCollection

This annotation indicates that the collection/array attribute of a given class is a direct
implicit collection than being child nodes under the attribute name. 

For example consider the following node hierarchy:

```
/content/mysite/stocks
                  |
                  +-- ADBE
                  +-- APPL
                  +-- GOOG 
```

The above can be modelled using the following class:

```java
public class MySite {

	public List<Stock> stocks;

}

JcrPerist.read("/content/mysite", MySite.class, resourceResolver);
```

Now if we assume that the stocks node had its own properties, either directly, or using a
`jcr:content` child node, we will miss on those properties. To facilitate the same, we can
add the `@AEMImplicitCollection` annotation to the attribute. Thus, our `MySite` class would
become:

```java
public class MySite {

	public Stocks stocks;
	
}

public class Stocks {

	// attributes for /content/mysite/stocks node may follow here

	@AEMImplicitCollection
	public List<Stocks> children;

}
```

The same can also be achieved using the following `JcrPerist` API:

```java
JcrPerist.setImplicitCollection(Stocks.class, "children");
```

## Extension points available

* `TypeInstantiator` that provides for creating instances that do not have a no-arguments constructor
* `ValueMapReader` for JCR objects that do not directly provide a `ValueMap` reading interface

### TypeInstantiator

### ValueMapReader
