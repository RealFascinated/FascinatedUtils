package cc.fascinated.fascinatedutils.common.types;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubAsset {
    private String name;
    @SerializedName("browser_download_url")
    private String browserDownloadUrl;
}
