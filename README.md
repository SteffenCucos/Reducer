# Reducer

Reducer is a Java/Maven experiment for compressing sets of related objects into larger non-overlapping “slices” of an N-dimensional categorical space.

The motivating case is a set of records where each record has several collection-valued dimensions, such as countries, cities, and populations. Reducer converts each object into a `Slice<T>`, merges adjacent or contained slices where possible, and converts the reduced slices back into domain objects.

## Concept

A `Slice<T>` represents a rectangular region in an N-dimensional space:

- each category/dimension maps to a collection of values;
- a point-slice contains one value in each category;
- a wider slice contains multiple values in one or more categories;
- two slices can be merged only when that merge does not introduce “holes” in the represented space.

For example, these two point-slices differ only by population:

```text
{country=[CA], city=[Toronto], population=[100]}
{country=[CA], city=[Toronto], population=[101]}
```

They can be reduced into:

```text
{country=[CA], city=[Toronto], population=[100, 101]}
```

The project is essentially exploring lossless reduction of sparse multidimensional data into a smaller set of larger rectangular regions.

## Implemented reducers

### `RecursiveReducer<T>`

A reflection-oriented reducer that can convert collection-valued fields into slices and recursively merge them. Earlier commits document the main invariant: input slices should have the same category structure, and the algorithm works best when the initial slices are point-slices.

### `NewRecursiveReducer<T>`

A newer recursive implementation that models the input as an N-dimensional space and tries to form the largest possible subregions without overlap. Its documented runtime is roughly proportional to the volume of the smallest containing super-slice multiplied by the density of the contained points.

### `DistanceReducer<T>`

A greedy reducer that compares slices by distance. If two slices differ in exactly one category, it can union that category and merge them. Later changes ordered candidate slices by slice volume to improve merge behavior.

### `MapReducer<K, V>`

A convenience reducer for maps whose values are collections. It converts a `Map<K, Collection<V>>` into a `Slice` and back again.

### `GraphDistanceReducer<T>` / `DistanceReducerV2<T>`

Experimental/partial reducer variants kept in the project for algorithm exploration.

## Project structure

```text
.
├── README.md
└── Reducer/
    ├── pom.xml
    └── src/
        ├── main/java/com/scucos/maven/Reducer/
        │   ├── Main.java
        │   ├── Slice.java
        │   └── Reducers/
        │       ├── Reducer.java
        │       ├── RecursiveReducer.java
        │       ├── NewRecursiveReducer.java
        │       ├── DistanceReducer.java
        │       ├── DistanceReducerV2.java
        │       ├── GraphDistanceReducer.java
        │       └── MapReducer.java
        └── test/java/
```

## Build

The project is a Maven JAR project under the nested `Reducer/` directory. It targets Java 8 and depends on JUnit 3.8.1 for tests and Guava 19.0.

```bash
cd Reducer
mvn test
mvn package
```

## Run the demo

`Main.java` contains a synthetic benchmark/demo using a nested `Region` class with three dimensions:

- countries
- cities
- populations

It builds full or sparse cubes of regions, runs a reducer, and prints reduction timing.

```bash
cd Reducer
mvn exec:java -Dexec.mainClass="com.scucos.maven.Reducer.Main"
```

If the Maven exec plugin is not configured locally, run `Main` from an IDE such as Eclipse or IntelliJ.

## Usage sketch

Create or select a reducer, then reduce a set of domain objects:

```java
Reducer<MyType> reducer = new NewRecursiveReducer<MyType>() {};
Set<MyType> reduced = reducer.reduce(items);
```

For custom object mappings, implement `toSlice` and `fromSlice` so the reducer knows how to move between the domain object and the multidimensional slice representation.

## Status

Experimental algorithm project. The code is useful as a prototype for multidimensional slice reduction, but the API and algorithms should be reviewed before being treated as a reusable library.

## Notes

- The repository uses an older Maven/Eclipse layout.
- The package namespace is `com.scucos.maven.Reducer`.
- Some reducers are incomplete or exploratory.
- The current implementation focuses on correctness and algorithm experimentation more than production packaging.

## License

No license has been selected yet.
