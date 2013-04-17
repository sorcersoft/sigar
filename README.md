sigar
=====

Sigar maven deployer

Usage
=====

```xml
	<dependencies>
		<dependency>
			<groupId>org.sorcersoft.sigar</groupId>
			<artifactId>sigar-maven</artifactId>
			<version>1.6.4</version>
		</dependency>
		<dependency>
			<groupId>org.sorcersoft.sigar</groupId>
			<artifactId>sigar</artifactId>
			<version>1.6.4</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>exec-maven-plugin</artifactId>
				<executions>
					<execution>
						<phase>install</phase>
						<goals>
							<goal>java</goal>
						</goals>
						<configuration>
							<executableDependency>
								<groupId>org.sorcersoft.sigar</groupId>
								<artifactId>sigar-maven</artifactId>
							</executableDependency>
							<mainClass>org.sorcersoft.sigar.Zip</mainClass>
							<arguments>
								<argument>org.sorcersoft.sigar:sigar:zip:native:1.6.4</argument>
							</arguments>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

Call java with -Djava.library.path=${user.home}/.m2/repository/org/sorcersoft/sigar/sigar/1.6.4/lib

Deployment
==========
    mvn deploy -DaltDeploymentRepository=...
    mvn exec:java -Ddeploy.repositoryId=... -Ddeploy.repositoryUrl=...

