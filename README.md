# **IDK Agent**

## **Overview**

The **IDK Agent** is a Java agent designed to help developers calculate the **deep memory footprint** of objects in a JVM. Unlike shallow memory calculators, this agent can traverse object fields to determine the memory used, considering references and nested structures up to a specified depth.

## Version 1.0.1-alpha
The current pre-release version of the IDK Agent is available on GitHub. You can download the JAR from the release page here:  
[IDK Agent v1.0.1.alpha Release](https://github.com/CodingAddict1530/idk-agent/releases/tag/v1.0.1-alpha)

This tool provides unparalleled flexibility by allowing users to:
- Specify the `depth` of the object traversal.
- Optionally `open modules` to access private and inaccessible fields for comprehensive memory analysis.
- Easily integrate the agent into their projects via a simple API.

---

## **Features**

- **Flexible Depth Measurement**: Measure objects' memory usage up to a specified depth, allowing targeted analysis of complex objects.
- **Module Accessibility**: Analyze memory usage for fields that may require module access.
- **Overloaded Method**: Choose the appropriate level of detail with multiple variations of the `getObjectSize` method.
- **Minimal Performance Overhead**: Optimized calculations for practical use in development and testing environments.

---

## **Installation**

1. **Add the Agent to Your Project**  
   To use the **IDK Agent**, include it in your project with the `-javaagent` JVM option when running your application. For example:
   ```bash
   java -javaagent:/path/to/idk-agent.jar -jar your-application.jar
   ```

2. **Ensure the Agent is Available**  
   The agent must be available in your project in one of the following ways:
   - If using Maven or Gradle, add it as a dependency to your project’s build configuration.
   - If using any other build system, make sure the agent JAR is on the classpath when running your application.

# **Usage**

## **Key Method**

The agent provides the following method for calculating object memory usage:

```bash
long getObjectSize(Object o, int depth, boolean openModules)
```

**Parameters**

- `Object o`: The object whose memory size you want to calculate.
- `int depth`: (Optional) The depth of traversal for object fields.
  - `0`: Only the object itself.
  - `1`: The object and the contents of its fields.
  - `2`: The object, its fields, and the fields of any referenced objects.
  - And so on.
- `boolean openModules`: (Optional) Whether to open modules to access private or inaccessible fields.
If `depth` and/or `openModules` are not provided, default values are used:
- `depth`: Integer.MAX_VALUE (traverse as deep as possible).
- `openModules`: true (open modules by default).

## **Overloaded Methods**

The agent provides multiple overloaded variations of getObjectSize:

```bash
long getObjectSize(Object o)
long getObjectSize(Object o, int depth)
long getObjectSize(Object o, boolean openModules)
long getObjectSize(Object o, int depth, boolean openModules)
```

## **Module Access**

If the `openModules` parameter is set to `true`, the agent will attempt to access private or inaccessible fields by opening modules. This is useful for measuring memory usage of fields that are normally inaccessible due to module encapsulation.

## **License**

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

