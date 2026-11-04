package com.example.demo;

import java.time.LocalDate;

/**
 * 誕生日情報を表すレコード。
 *
 * @param id 登録された情報の一意な識別子
 * @param name ユーザーの名前
 * @param birthDate 誕生日
 */
public record Person(int id, String name, LocalDate birthDate) {
}