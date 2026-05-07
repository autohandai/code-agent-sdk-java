# Memory

Autohand CLI memory is exposed through tool events. The Java SDK does not write
memory directly; prompt the agent and observe when it calls memory tools.

```java
sdk.streamPrompt(new PromptParams("Remember that this team uses Java 21."), event -> {
    if (event instanceof Events.ToolStartEvent e && "save_memory".equals(e.toolName())) {
        System.out.println("Saving memory");
    }
});
```

Ask explicitly when you want memory recalled:

```java
sdk.streamPrompt(new PromptParams("What Java preferences do you remember?"), event -> {
    if (event instanceof Events.MessageUpdateEvent e) {
        System.out.print(e.delta());
    }
});
```
