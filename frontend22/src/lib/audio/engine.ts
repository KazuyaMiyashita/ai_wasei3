export class AudioEngine {
  private context: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private nodes: Map<number, { osc: OscillatorNode; gain: GainNode }> =
    new Map();

  get currentTime(): number {
    return this.context ? this.context.currentTime : 0;
  }

  get state(): AudioContextState {
    return this.context ? this.context.state : "closed";
  }

  init(): AudioContext {
    if (!this.context) {
      const AudioCtor =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext })
          .webkitAudioContext;
      this.context = new AudioCtor();

      this.masterGain = this.context.createGain();
      this.masterGain.connect(this.context.destination);
      this.masterGain.gain.value = 0.5; // Overall volume
    } else if (this.context.state === "suspended") {
      this.context.resume();
    }
    return this.context;
  }

  stopAll(fadeOutDuration = 0.05) {
    if (!this.context) return;

    const now = this.context.currentTime;
    this.nodes.forEach(({ osc, gain }) => {
      try {
        gain.gain.cancelScheduledValues(now);
        gain.gain.setValueAtTime(gain.gain.value, now);
        gain.gain.linearRampToValueAtTime(0, now + fadeOutDuration);
        osc.stop(now + fadeOutDuration);
      } catch (_e) {
        // Ignore errors if already stopped
      }
    });
    this.nodes.clear();
  }

  schedulePart(
    id: number,
    startTime: number,
    pitch: { time: number; value: number }[],
    intensity: { time: number; value: number }[],
    stopTime: number,
  ) {
    if (!this.context || !this.masterGain) return;

    const osc = this.context.createOscillator();
    const gain = this.context.createGain();

    // Create a custom periodic wave for additive synthesis
    // Fundamental + 4 harmonics
    const real = new Float32Array([0, 1, 0.1, 0.05, 0.05, 0.01]);
    const imag = new Float32Array([0, 0, 0, 0, 0, 0]);
    const wave = this.context.createPeriodicWave(real, imag);

    osc.setPeriodicWave(wave);
    osc.connect(gain);
    gain.connect(this.masterGain);

    // Schedule Pitch
    if (pitch.length > 0) {
      osc.frequency.setValueAtTime(
        pitch[0].value,
        startTime + pitch[0].time / 1000,
      );
      for (let i = 1; i < pitch.length; i++) {
        const point = pitch[i];
        const prevPoint = pitch[i - 1];
        const time = startTime + point.time / 1000;

        // Maintain previous value until 1ms before the target time
        if (point.time - prevPoint.time > 1) {
          osc.frequency.setValueAtTime(prevPoint.value, time - 0.001);
        }
        osc.frequency.linearRampToValueAtTime(point.value, time);
      }
    }

    // Schedule Intensity
    gain.gain.setValueAtTime(0, startTime);

    if (intensity.length > 0) {
      intensity.forEach((point, i) => {
        const time = startTime + point.time / 1000;
        const linear = point.value <= -90 ? 0 : 10 ** (point.value / 20);

        if (i > 0) {
          const prevPoint = intensity[i - 1];
          const prevLinear =
            prevPoint.value <= -90 ? 0 : 10 ** (prevPoint.value / 20);

          // Maintain previous value until 1ms before the target time
          if (point.time - prevPoint.time > 1) {
            gain.gain.setValueAtTime(prevLinear, time - 0.001);
          }
        }

        gain.gain.linearRampToValueAtTime(linear, time);
      });
    }

    osc.start(startTime);
    osc.stop(stopTime);

    this.nodes.set(id, { osc, gain });
  }

  close() {
    if (this.context) {
      this.stopAll(0);
      this.context.close();
      this.context = null;
      this.masterGain = null;
    }
  }
}
