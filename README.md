# stream-rest-template
A simple extension to Spring's RestTemplate which enables streaming support. This behaviour is usually required when receiving a very large (or unknown sized) response and dev would like to:
1. handle response as soon as it arrives, instead of waiting until request finishes.
2. not caching the entire response in-memory, and thus avoiding OutOfMemory issues.

In order to use this extension, one should specify InputStreamResource.class as the response type.

* This extension does not work on \*ForObject methods, use \*ForEntity instead.

Note: it is also possible with vanilla RestTemplate to specify an InputStreamResource object as response type, however due to a bug/design flaw RestTemplate closes the connection before returning the response, making the returned input stream useless.


