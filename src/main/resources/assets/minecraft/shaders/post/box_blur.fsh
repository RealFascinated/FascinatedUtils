#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

// Modified by FascinatedUtils: replaced the vanilla one-sided bilinear loop with a
// symmetric integer-offset loop. The vanilla loop (a = -r+0.5 to r, step 2) assumes
// integer r: with fractional r the iteration count changes mid-animation, producing an
// asymmetric kernel whose centroid oscillates → screen shake. The new loop samples
// symmetric ±i pairs with uniform weight, then adds fractional boundary pixels at
// ±(intR+1) for smooth sub-integer transitions. Pixel 0 is the exact center.
void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;

    float actualRadius = Radius >= 0.5 ? Radius : MenuBlurRadius;
    float intR = floor(actualRadius);
    float frac = actualRadius - intR;

    // Center pixel
    vec4 blurred = texture(InSampler, texCoord);
    float weight = 1.0;

    // Symmetric integer-offset pairs: kernel is uniform over [-intR, intR]
    for (float i = 1.0; i <= intR; i += 1.0) {
        blurred += texture(InSampler, texCoord + sampleStep * i)
                 + texture(InSampler, texCoord - sampleStep * i);
        weight += 2.0;
    }

    // Fractional boundary: pixels at ±(intR+1) fade in smoothly as frac grows
    blurred += (texture(InSampler, texCoord + sampleStep * (intR + 1.0))
              + texture(InSampler, texCoord - sampleStep * (intR + 1.0))) * frac;
    weight += 2.0 * frac;

    fragColor = blurred / weight;
}
