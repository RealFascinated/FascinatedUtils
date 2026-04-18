#version 150

// Imported from Vibrant Vanilla and adapted for FascinatedUtils post-processing.
const vec4 coeff = vec4(0.299, 0.587, 0.114, 0.0);

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 inTexel = texture(DiffuseSampler, texCoord);
    float vibrancy = 0.4;

    float lum = dot(inTexel, coeff);
    vec4 mask = clamp(inTexel - vec4(lum), 0.0, 1.0);
    float lumMask = 1.0 - dot(coeff, mask);
    float mixAmount = clamp(1.0 + vibrancy * lumMask, 0.0, 2.0);
    vec4 gray = vec4(vec3(lum), 1.0);

    fragColor = vec4(mix(gray, inTexel, mixAmount).rgb, 1.0);
}
