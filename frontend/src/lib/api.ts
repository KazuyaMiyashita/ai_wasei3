import { FullScore } from "./model";
import type { components } from "./schema";

const API_BASE_URL = "http://localhost:8000";

export type GenerateRequest = components["schemas"]["CounterpointRequest"];

export async function generateCounterpoint(
  requestData: GenerateRequest,
): Promise<FullScore> {
  const response = await fetch(`${API_BASE_URL}/counterpoint`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(requestData),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `API Error: ${response.status} ${response.statusText}\n${errorText}`,
    );
  }

  const json = await response.json();
  return FullScore.fromJSON(json);
}
