package cc.fascinated.fascinatedutils.common;

public class AlumiteEnvironment {

    public static final String API_BASE_URL = System.getenv().getOrDefault("ALUMITE_API_URL", "https://alumite-api.fascinated.cc");
}
