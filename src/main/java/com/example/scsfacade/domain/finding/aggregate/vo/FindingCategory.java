package com.example.scsfacade.domain.finding.aggregate.vo;

/**
 * 弱點分類與類型 (FindingCategory) 列舉。
 * <p>
 * 區分源碼真實漏洞、代碼壞味道、邏輯缺陷與安全熱點。
 * </p>
 */
public enum FindingCategory {

    /**
     * 安全漏洞 (Vulnerability)：潛在可被利用之安全缺陷 (如 SQL 注入、XSS、RCE)。
     */
    VULNERABILITY,

    /**
     * 程式碼錯誤 (Bug)：可能導致執行時期拋出異常或邏輯錯誤之缺陷 (如 NPE、資源未釋放)。
     */
    BUG,

    /**
     * 程式碼壞味道 (Code Smell)：影響系統可維護性、可讀性或架構壞味道之程式碼問題。
     */
    CODE_SMELL,

    /**
     * 安全熱點 (Security Hotspot)：具備潛在資安敏感性但需人工審核判斷之代碼區段 (如加解密演算法使用)。
     */
    SECURITY_HOTSPOT,

    /**
     * 機敏資訊洩漏 (Secret Leak)：程式碼中硬編碼之密碼、API 金鑰或私鑰等憑證。
     */
    SECRET_LEAK,

    /**
     * 軟體相依套件漏洞 (SCA)：第三方套件相依庫中所存在之已知安全漏洞 (如 CVE)。
     */
    SCA
}
