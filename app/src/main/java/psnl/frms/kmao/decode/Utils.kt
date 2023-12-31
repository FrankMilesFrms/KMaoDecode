/*
 * Copyright (C) 2023 Frank Miles - Frms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psnl.frms.kmao.decode

import android.util.Base64
import cn.hutool.core.io.FileUtil
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.PriorityQueue
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 *
 * @author Frms(Frank Miles)
 * @email 3505826836@qq.com
 * @time 2023/08/19 下午 11:38
 */
fun check(input: String, output: String): Pair<StringBuilder, Boolean>
{
	var flag = true
	val stringBuilder = StringBuilder()
	val inputFile = File(input)
	
	
	if(inputFile.canRead().not()) {
		flag = false
		stringBuilder.append("输入文件夹：\n").append(input).append("\n不可读！")
	}

	if(inputFile.isDirectory.not()) {
		flag = false
		stringBuilder.append("输入路径：\n").append(input).append("\n不是文件夹！")
	}
	
	if(flag && stringBuilder.isNotEmpty()) {
		stringBuilder.append('\n')
	}
	
	val outputFile = File(output)
	
	if(outputFile.parentFile?.canRead()?.not() == true)
	{
		flag = false
		stringBuilder.append("输出文件所在路径：\n").append(output).append("\n不可读！")
	}

	if(outputFile.isFile) {
		flag = false
		stringBuilder.append("输出文件：\n").append(output).append("\n已经存在了！")
	}

	if(outputFile.isDirectory) {
		flag = false
		stringBuilder.append("输出的路径：\n").append(output).append("\n是文件夹，请添写另一个名字作为文件！")
	}
	if(flag) {
		stringBuilder.append("输出、输出文件填写正确，可以生成了！")
	}
	return stringBuilder to flag
}

@Throws(Exception::class)
@Synchronized
fun decrypt(str: String, key : String, option:String): ByteArray
{
	return try
	{
		val decode = Base64.decode(str.toByteArray(charset("UTF-8")), 0)
		val ivParameterSpec = IvParameterSpec(decode, 0, 16)
		val copyOfRange = Arrays.copyOfRange(decode, 16, decode.size)
		val cipher = Cipher.getInstance(option)
		cipher.init(2, SecretKeySpec(key.toByteArray(), "AES"), ivParameterSpec)
		cipher.doFinal(copyOfRange)
	} catch (unused: Exception) { ByteArray(size = 0) }
}

/**
 * if not contains point, mean the files are do not lexer.
 * @receiver String
 * @param b String
 * @return Int
 */
fun String.lexerCompareTxt(b: String) : Int
{
	val txt = '.'
	if(this.contains(txt) and b.contains(txt))
	{
		return getPrefix().toLong().compareTo(b.getPrefix().toLong())
	}
	
	return compareTo(b)
}

fun String.getPrefix(): String = substring(0, indexOf('.'))

/**
 * 构建小说
 * @param input File 输入
 * @param output File 输出
 * @param key String 密匙
 * @param option String 解密算法
 * @param saveInfo Boolean debug模式
 * @param lowMemoryMode Boolean 低内存模式
 * @param autoFixPrefix Pair<String, Int> 自动追加内容，
 * 在文章开头追加int位String，如果int为负，则补全文章开头的String至(-int)位
 * @return String
 */
fun buildNovel(
		input: File,
		output: File,
		key : String,
		option:String,
		saveInfo: Boolean,
		lowMemoryMode: Boolean,
//		autoFixPrefix: Pair<String, Int>
): String
{
	var fileSize : Long = 0
	var message = ""
	var count = 1
	val str = StringBuffer()
	val executorService = Executors.newSingleThreadExecutor()
	val queue = PriorityQueue { a: File, b: File ->
		a.name.lexerCompareTxt(b.name)
	}
	input.listFiles()?.let { queue.addAll(it) }
	
	while (queue.isNotEmpty())
	{
		val poll = queue.poll()!!

		// 避免文件夹被读取
		if(!poll.isFile) {
			continue
		}
		executorService.submit {

			val f = FileUtil.readLines(
					poll,
					StandardCharsets.UTF_8
			)
			val stringBuilder = java.lang.StringBuilder()
			f.forEach(stringBuilder::append)
			
			val p: ByteArray = decrypt(stringBuilder.toString(), key, option)
			
			if (p.isEmpty()) {
				message += ("${poll.absoluteFile.name}   无法读取，错误文件！\n")
			} else
			{
				str.append('第').append(count).append("章\n")
						.apply {
							if(saveInfo)
							{
								append("文件名：").append(poll.name).append('\n')
								append("绝对路径：").append(poll.absoluteFile).append('\n')
							}
						}
						.append(String(p)).append('\n')
				
				fileSize += str.length
				
				if(lowMemoryMode)
				{
//					autoPrefix(str, autoFixPrefix)
					FileUtil.appendUtf8String(str.toString(), output)
					str.delete(0, str.length)
				}
				count++
			}
		}
	}
	
	executorService.shutdown()
	
	while (true)
	{
		if(executorService.isTerminated )
		{
			if(lowMemoryMode.not())
			{
				//				autoPrefix(str, autoFixPrefix)
				FileUtil.writeString(
					str.toString(),
					output,
					StandardCharsets.UTF_8)
			}

			break
		}
	}
	message += ("结束！已经写入章节数"+(count-1))
	return message
}

///**
// *自动追加内容
// * @param str StringBuffer
// * @param autoFixPrefix Pair<String, Int> 在文章开头追加int位String，
// * 如果int为负，则补全文章开头的String至(-int)位
// */
//fun autoPrefix(
//		str: StringBuffer,
//		autoFixPrefix: Pair<String, Int>,
//)
//{
//	if(autoFixPrefix.second == 0) {
//		return
//	}
//	val deque = ArrayDeque<String>()
//	var start = 0
//	for(index in str.indices)
//	{
//		if(str[index] == '\n') {
//			deque.addLast(str.substring(start, index))
//		}
//	}
//}