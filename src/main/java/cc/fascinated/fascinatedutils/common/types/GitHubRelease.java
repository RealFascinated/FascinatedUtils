package cc.fascinated.fascinatedutils.common.types;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GitHubRelease {
    private String tagName;
    private String name;
    private List<GitHubAsset> assets;
}
