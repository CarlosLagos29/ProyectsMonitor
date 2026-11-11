type MessageTree = { [key: string]: string | MessageTree }

export function defineMessages<T extends MessageTree>(messages: { en: T; es: NoInfer<T> }) {
  return messages
}
