```java
public class JWTVerificationExplanation {
    
    public static void main(String[] args) {
        explainJWTStructure();
        explainVerificationProcess();
        compareWithEmailVerification();
    }
    
    /**
     * JWT 구조 설명
     */
    public static void explainJWTStructure() {
        System.out.println("=== JWT 구조 ===");
        System.out.println("JWT = Header.Payload.Signature");
        System.out.println();
        
        String exampleJWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjI1MDk3NjAwfQ.signature";
        String[] parts = exampleJWT.split("\\.");
        
        System.out.println("Header:    " + parts[0]);
        System.out.println("Payload:   " + parts[1]); 
        System.out.println("Signature: " + parts[2]);
        System.out.println();
        
        // Header 디코딩 (Base64)
        System.out.println("Header 내용: {\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        System.out.println("Payload 내용: {\"sub\":\"user123\",\"exp\":1625097600}");
        System.out.println("Signature: HMAC-SHA256(Header + Payload, SecretKey)");
    }
    
    /**
     * JWT 검증 과정 설명
     */
    public static void explainVerificationProcess() {
        System.out.println("\n=== JWT 검증 과정 ===");
        System.out.println("1. 받은 JWT를 . 기준으로 분리");
        System.out.println("2. Header + Payload를 시크릿키로 다시 서명");  
        System.out.println("3. 새로 만든 서명 vs 받은 서명 비교");
        System.out.println("4. 일치하면 → 유효한 토큰");
        System.out.println("5. 다르면 → 위조된 토큰");
        System.out.println();
        
        System.out.println("🔍 핵심: 전체 토큰을 다시 만드는 게 아니라");
        System.out.println("         서명 부분만 다시 계산해서 비교!");
    }
    
    /**
     * 이메일 인증 토큰과의 차이점
     */
    public static void compareWithEmailVerification() {
        System.out.println("\n=== AccessToken vs 이메일인증토큰 비교 ===");
        System.out.println();
        
        System.out.println("📧 이메일 인증 토큰:");
        System.out.println("   - JWT + DB 저장");
        System.out.println("   - 검증: JWT 파싱 + DB에서 토큰 존재 확인");
        System.out.println("   - 일회성 (한번 사용하면 DB에서 삭제/무효화)");
        System.out.println("   - 보안: JWT 서명 + DB 검증 (이중 보안)");
        System.out.println();
        
        System.out.println("🔑 AccessToken:");
        System.out.println("   - JWT만 사용 (DB 저장 안 함)");
        System.out.println("   - 검증: JWT 서명 검증만");
        System.out.println("   - 재사용 가능 (만료될 때까지)");
        System.out.println("   - 보안: JWT 서명만 (stateless)");
    }
}

/**
 * 실제 JWT 검증 코드 예시
 */
class JWTVerificationCode {
    
    /**
     * ❌ 이런 방식이 아님!
     */
    public boolean wrongVerification(String receivedToken, String secretKey) {
        // 받은 토큰을 파싱해서 payload 추출
        // payload로 새 토큰 생성
        // 전체 토큰 비교
        // → 이런 방식 아님!
        return false;
    }
    
    /**
     * ✅ 실제 JWT 검증 방식
     */
    public boolean correctVerification(String receivedToken, String secretKey) {
        try {
            // 1. 토큰을 3부분으로 분리
            String[] parts = receivedToken.split("\\.");
            String header = parts[0];
            String payload = parts[1];
            String receivedSignature = parts[2];
            
            // 2. Header + Payload를 시크릿키로 다시 서명
            String newSignature = hmacSha256(header + "." + payload, secretKey);
            
            // 3. 서명 비교
            return receivedSignature.equals(newSignature);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * JJWT 라이브러리를 사용한 실제 검증
     */
    public Claims verifyWithJJWT(String token, String secretKey) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)  // ← 여기서 서명 검증 자동 수행
                    .getBody();
                    
        } catch (JwtException e) {
            // 서명이 틀리면 예외 발생
            throw new IllegalArgumentException("Invalid token");
        }
    }
    
    // 실제 HMAC-SHA256 구현 (예시용)
    private String hmacSha256(String data, String key) {
        // HMAC-SHA256 계산 로직
        return "calculated-signature";
    }
}
```