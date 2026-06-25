import { Injectable } from '@angular/core';

interface SpeechBlipOptions {
  intervalMs?: number;
  startLeadMs?: number;
}

@Injectable({ providedIn: 'root' })
export class TamiSpeechAudioService {
  private audioContext?: AudioContext;
  private initialTimer?: ReturnType<typeof setTimeout>;
  private intervalTimer?: ReturnType<typeof setInterval>;
  private warmedUp = false;
  private readonly defaultIntervalMs = 72;
  private readonly defaultStartLeadMs = 180;
  private readonly minAudibleLatencyMs = 120;
  private readonly maxAudibleLatencyMs = 260;
  private readonly latencySafetyMs = 140;
  private readonly volume = 0.12;

  async prepare(): Promise<boolean> {
    if (typeof window === 'undefined') {
      return false;
    }

    const context = this.getAudioContext();
    if (!context || context.state === 'closed') {
      return false;
    }

    if (context.state === 'suspended') {
      try {
        await context.resume();
      } catch {
        return false;
      }
    }

    if (context.state !== 'running') {
      return false;
    }

    await this.warmUpOutput(context);
    return true;
  }

  async start(options: SpeechBlipOptions = {}): Promise<boolean> {
    this.stop();

    const ready = await this.prepare();
    if (!ready) {
      return false;
    }

    const intervalMs = Math.max(48, options.intervalMs ?? this.defaultIntervalMs);
    const startLeadMs = Math.max(120, options.startLeadMs ?? this.defaultStartLeadMs);
    const audibleLatencyMs = this.estimateAudibleLatencyMs();

    this.playBlip(startLeadMs / 1000);

    this.initialTimer = setTimeout(() => {
      this.intervalTimer = setInterval(() => this.playBlip(), intervalMs);
    }, startLeadMs + intervalMs);

    await this.sleep(startLeadMs + audibleLatencyMs);
    return true;
  }

  stop(): void {
    if (this.initialTimer) {
      clearTimeout(this.initialTimer);
      this.initialTimer = undefined;
    }

    if (this.intervalTimer) {
      clearInterval(this.intervalTimer);
      this.intervalTimer = undefined;
    }
  }

  private getAudioContext(): AudioContext | undefined {
    if (this.audioContext) {
      return this.audioContext;
    }

    const AudioContextConstructor = window.AudioContext ?? window.webkitAudioContext;
    if (!AudioContextConstructor) {
      return undefined;
    }

    this.audioContext = new AudioContextConstructor();
    return this.audioContext;
  }

  private estimateAudibleLatencyMs(): number {
    const context = this.audioContext;
    if (!context) {
      return this.minAudibleLatencyMs + this.latencySafetyMs;
    }

    const contextWithOutputLatency = context as AudioContext & { outputLatency?: number };
    const knownLatencySeconds = (context.baseLatency ?? 0) + (contextWithOutputLatency.outputLatency ?? 0);
    const knownLatencyMs = Math.ceil(knownLatencySeconds * 1000);

    return Math.min(
      this.maxAudibleLatencyMs,
      Math.max(this.minAudibleLatencyMs, knownLatencyMs + this.latencySafetyMs)
    );
  }

  private async warmUpOutput(context: AudioContext): Promise<void> {
    if (this.warmedUp) {
      return;
    }

    const now = context.currentTime;
    const oscillator = context.createOscillator();
    const gain = context.createGain();

    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(440, now);
    gain.gain.setValueAtTime(0.00001, now);

    oscillator.connect(gain);
    gain.connect(context.destination);

    oscillator.start(now);
    oscillator.stop(now + 0.012);

    this.warmedUp = true;
    await this.sleep(40);
  }

  private playBlip(startOffsetSeconds = 0): void {
    const context = this.audioContext;
    if (!context || context.state !== 'running') {
      return;
    }

    const now = context.currentTime + Math.max(0, startOffsetSeconds);
    const duration = 0.038;
    const baseFrequency = 780 + Math.random() * 260;
    const endFrequency = baseFrequency * (1.25 + Math.random() * 0.3);

    const primary = context.createOscillator();
    primary.type = 'triangle';
    primary.frequency.setValueAtTime(baseFrequency, now);
    primary.frequency.exponentialRampToValueAtTime(endFrequency, now + duration);

    const sparkle = context.createOscillator();
    sparkle.type = 'sine';
    sparkle.frequency.setValueAtTime(baseFrequency * 2.02, now);
    sparkle.frequency.exponentialRampToValueAtTime(endFrequency * 2.08, now + duration * 0.85);

    const gain = context.createGain();
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(this.volume, now + 0.004);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);

    const filter = context.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(1300, now);
    filter.Q.setValueAtTime(5, now);

    primary.connect(filter);
    sparkle.connect(filter);
    filter.connect(gain);
    gain.connect(context.destination);

    primary.start(now);
    sparkle.start(now);
    primary.stop(now + duration + 0.01);
    sparkle.stop(now + duration + 0.01);
  }

  private sleep(milliseconds: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
  }
}

declare global {
  interface Window {
    webkitAudioContext?: typeof AudioContext;
  }
}
