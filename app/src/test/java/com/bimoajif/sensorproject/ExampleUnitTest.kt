package com.bimoajif.sensorproject

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun sales_by_match() {
        println("~~~~~~~~ START ~~~~~~~~")

        val temp: MutableList<Int> = mutableListOf()

        val n = 7
        val ar : IntArray = intArrayOf(1,2,1,2,1,3,2)

        for (i in 0..<n) {
            for (j in i+1..<n) {
                if(ar[i] == ar[j]){
                    if(!temp.contains(element = i) && !temp.contains(element = j)) {
                        temp.add(i)
                        temp.add(j)
                    }
                }
            }
        }
        println(ar.toList())
        println(temp.count()/2)

        println("~~~~~~~~ END ~~~~~~~~")
    }

    @Test
    fun counting_valley() {
        println("~~~~~~~~ START ~~~~~~~~")

        var temp = 0
        var sum = 0

        val n = 8
        val path = "DDUUUUDD"

        for(i in 0..<n) {
                when (path[i]) {
                    'D' -> {
                        temp--
                    }
                    'U' -> {
                        temp++
                        if(temp == 0) {
                            sum++
                        }
                }
            }
        }
        println(sum)

        println("~~~~~~~~ END ~~~~~~~~")
    }

    @Test
    fun jumping_on_the_cloud(){
        println("~~~~~~~~ START ~~~~~~~~")

        var i = 0
        var count = 0
        val c : IntArray = intArrayOf(0,0,1,0,0,1,0)

        while (i < c.count()) {
            if(c[i] != 1) {
                if(i+1 != c.count() && c[i+1] != 1)  {
                    i++
                }
                count++
            }
            i++
        }

        println(count)

        println("~~~~~~~~ END ~~~~~~~~")
    }
}
