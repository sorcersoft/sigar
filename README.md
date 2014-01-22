sigar
=====

Sigar maven deployer

Usage
=====

```xml
<dependencies>
	<dependency>
		<groupId>org.sorcersoft.sigar</groupId>
		<artifactId>sigar</artifactId>
		<version>1.6.4-2</version>
	</dependency>
</dependencies>

```

unzip sigar-native to a directory designated for native libraries
Call java with -Djava.library.path=<native libraries directory>

Deployment
==========
```bash
mvn exec:java -DaltDeploymentRepository=...
```
